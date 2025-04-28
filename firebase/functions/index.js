const functions = require('firebase-functions');
const admin = require('firebase-admin');
const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);

admin.initializeApp();

/**
 * Webhook handler for Stripe payments
 * This function receives webhook events from Stripe when payments are completed
 * Configure this in Stripe Dashboard → Developers → Webhooks
 */
exports.stripeWebhook = functions.https.onRequest(async (req, res) => {
  const sig = req.headers['stripe-signature'];
  const endpointSecret = process.env.STRIPE_WEBHOOK_SECRET;
  
  let event;
  
  try {
    // Verify the event came from Stripe using the signature and secret
    if (endpointSecret) {
      event = stripe.webhooks.constructEvent(req.rawBody, sig, endpointSecret);
    } else {
      // For development only - don't use this in production
      event = req.body;
    }
  } catch (err) {
    console.error(`Webhook signature verification failed: ${err.message}`);
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }
  
  // Handle the event based on its type
  switch (event.type) {
    case 'checkout.session.completed':
      // Payment was successful
      const session = event.data.object;
      
      // Get the customer and client_reference_id (user ID from your app)
      const clientReferenceId = session.client_reference_id;
      const customerId = session.customer;
      const metadata = session.metadata || {};
      
      // Skip if no user ID was provided
      if (!clientReferenceId) {
        console.log('No client_reference_id found in the session');
        return res.status(400).send('Missing client_reference_id');
      }
      
      // Get subscription type from metadata or try to determine from the price
      const priceId = session.line_items?.data[0]?.price?.id || 
                     metadata.priceId || 
                     session.line_items?.data[0]?.price;
      
      // Determine subscription type from the price or amount
      let subscriptionType;
      
      if (metadata.subscriptionType) {
        // If we explicitly set the subscription type in metadata
        subscriptionType = metadata.subscriptionType;
      } else {
        // Try to guess based on the amount
        const amountTotal = session.amount_total || 0;
        // Determine based on price - adjust these amounts to match your pricing
        subscriptionType = amountTotal < 1000 ? 'MONTHLY' : 'YEARLY';
      }
      
      // Create a record of the transaction in Firestore
      const transactionId = session.id; // Use Stripe session ID as transaction ID
      const now = admin.firestore.FieldValue.serverTimestamp();
      
      // Calculate expiration date
      const expireDate = new Date();
      if (subscriptionType === 'MONTHLY') {
        expireDate.setMonth(expireDate.getMonth() + 1);
      } else if (subscriptionType === 'YEARLY') {
        expireDate.setFullYear(expireDate.getFullYear() + 1);
      }
      
      // Store transaction in the pending transactions collection
      try {
        await admin.firestore().collection('pending_transactions').doc(transactionId).set({
          userId: clientReferenceId,
          status: 'approved',
          subscriptionType: subscriptionType,
          customerId: customerId,
          purchaseDate: now,
          expirationDate: expireDate.getTime(),
          amount: session.amount_total,
          currency: session.currency
        });
        
        console.log(`✅ Successfully stored transaction: ${transactionId} for user: ${clientReferenceId}`);
      } catch (error) {
        console.error(`❌ Error storing transaction: ${error.message}`);
        return res.status(500).send(`Error storing transaction: ${error.message}`);
      }
      
      break;
      
    // Handle other event types if needed
    case 'checkout.session.async_payment_succeeded':
      // Handle asynchronous payment success
      console.log('Async payment succeeded');
      break;
      
    case 'checkout.session.async_payment_failed':
      // Handle asynchronous payment failure
      console.log('Async payment failed');
      break;
      
    default:
      // Unexpected event type
      console.log(`Unhandled event type: ${event.type}`);
  }
  
  // Successfully processed the webhook, send success response
  res.json({received: true});
}); 