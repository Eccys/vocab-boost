const functions = require('firebase-functions');
const admin = require('firebase-admin');
const stripe = require('stripe')(functions.config().stripe.secret_key);

admin.initializeApp();

/**
 * Securely verifies a Stripe payment using the Stripe API.
 * This keeps the Stripe secret key secure on the server side.
 */
exports.verifyStripePayment = functions.https.onCall(async (data, context) => {
  // Ensure the user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }

  const { transactionId, userId } = data;
  
  // Validate inputs
  if (!transactionId || typeof transactionId !== 'string') {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Transaction ID must be a non-empty string.'
    );
  }
  
  // Check if user ID matches authenticated user
  if (context.auth.uid !== userId) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'You can only verify payments for your own account.'
    );
  }
  
  try {
    // First check our database to see if this payment was already verified
    const db = admin.firestore();
    const transactionDoc = await db.collection('transactions').doc(transactionId).get();
    
    if (transactionDoc.exists) {
      const data = transactionDoc.data();
      // If already verified and belongs to this user, return success
      if (data.userId === userId && data.status === 'verified') {
        return { 
          verified: true,
          subscriptionType: data.subscriptionType,
          message: "Payment previously verified."
        };
      }
      
      // If belongs to another user, reject
      if (data.userId && data.userId !== userId) {
        return {
          verified: false,
          error: "This payment ID has already been used by another account."
        };
      }
    }
    
    // Check with Stripe API based on the format of the ID
    let stripeObject;
    let paymentAmount = 0;
    let subscriptionType = null;
    
    // Determine which API to call based on ID prefix
    if (transactionId.startsWith('pi_')) {
      // It's a payment intent
      stripeObject = await stripe.paymentIntents.retrieve(transactionId);
      
      // Verify the payment was successful
      if (stripeObject.status !== 'succeeded') {
        return {
          verified: false,
          error: `Payment is not completed. Status: ${stripeObject.status}`
        };
      }
      
      paymentAmount = stripeObject.amount;
    } else if (transactionId.startsWith('cs_')) {
      // It's a checkout session
      stripeObject = await stripe.checkout.sessions.retrieve(transactionId);
      
      // Verify the session was paid
      if (stripeObject.payment_status !== 'paid') {
        return {
          verified: false,
          error: `Payment is not completed. Status: ${stripeObject.payment_status}`
        };
      }
      
      paymentAmount = stripeObject.amount_total;
    } else {
      // For testing purposes only
      if (transactionId.startsWith('test_') && process.env.NODE_ENV !== 'production') {
        return {
          verified: true,
          subscriptionType: 'MONTHLY',
          message: "Test payment accepted. Not verified with Stripe."
        };
      }
      
      return {
        verified: false,
        error: "Unrecognized payment ID format."
      };
    }
    
    // Determine subscription type based on amount
    if (paymentAmount >= 5000) { // $50.00 or more (in cents)
      subscriptionType = 'YEARLY';
    } else {
      subscriptionType = 'MONTHLY';
    }
    
    // Save verification result to Firestore
    const verificationData = {
      userId: userId,
      transactionId: transactionId,
      verified: true,
      subscriptionType: subscriptionType,
      verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      stripeStatus: stripeObject.status || stripeObject.payment_status,
      amount: paymentAmount
    };
    
    await db.collection('payment_verifications').doc(transactionId).set(verificationData);
    
    return {
      verified: true,
      subscriptionType: subscriptionType,
      message: "Payment successfully verified with Stripe."
    };
    
  } catch (error) {
    console.error("Stripe verification error:", error);
    
    // Handle different Stripe errors appropriately
    if (error.type === 'StripeInvalidRequestError') {
      return {
        verified: false,
        error: "Invalid payment ID. The transaction could not be found."
      };
    }
    
    return {
      verified: false,
      error: "Verification failed: " + error.message
    };
  }
}); 