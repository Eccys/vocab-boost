import { serve } from 'https://deno.land/std@0.177.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import Stripe from 'https://esm.sh/stripe@12.0.0?target=deno'

// Stripe initialization
const stripe = new Stripe(Deno.env.get('STRIPE_SECRET_KEY') || '', {
  apiVersion: '2023-10-16', // Update to latest Stripe API version
})

serve(async (req) => {
  try {
    // Create a Supabase client with the Auth context of the logged in user
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    // Get the JWT token from the request
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(
        JSON.stringify({
          error: 'Missing Authorization header',
        }),
        { status: 401, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Verify the user is authenticated
    const {
      data: { user },
      error: authError,
    } = await supabaseClient.auth.getUser()

    if (authError || !user) {
      return new Response(
        JSON.stringify({
          error: 'Unauthorized',
        }),
        { status: 401, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Parse the request body
    const { transactionId, userId } = await req.json()

    // Validate inputs
    if (!transactionId || typeof transactionId !== 'string') {
      return new Response(
        JSON.stringify({
          error: 'Transaction ID must be a non-empty string',
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check if user ID matches authenticated user
    if (user.id !== userId) {
      return new Response(
        JSON.stringify({
          error: 'You can only verify payments for your own account',
        }),
        { status: 403, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // First check database to see if this payment was already verified
    const { data: transactionData, error: transactionError } = await supabaseClient
      .from('transactions')
      .select('*')
      .eq('id', transactionId)
      .single()

    if (transactionError && transactionError.code !== 'PGRST116') {
      // Error other than 'not found'
      console.error('Database error:', transactionError)
      return new Response(
        JSON.stringify({
          error: 'Database error occurred',
        }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    if (transactionData) {
      // Transaction exists
      if (transactionData.user_id === userId && transactionData.status === 'verified') {
        // Already verified for this user
        return new Response(
          JSON.stringify({
            verified: true,
            subscriptionType: transactionData.subscription_type,
            message: 'Payment previously verified',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        )
      }

      // Used by another user
      if (transactionData.user_id && transactionData.user_id !== userId) {
        return new Response(
          JSON.stringify({
            verified: false,
            error: 'This payment ID has already been used by another account',
          }),
          { status: 400, headers: { 'Content-Type': 'application/json' } }
        )
      }
    }

    // Check with Stripe API based on ID format
    let stripeObject
    let paymentAmount = 0
    let subscriptionType = null

    // Determine which API to call based on ID prefix
    if (transactionId.startsWith('pi_')) {
      // It's a payment intent
      stripeObject = await stripe.paymentIntents.retrieve(transactionId)

      // Verify the payment was successful
      if (stripeObject.status !== 'succeeded') {
        return new Response(
          JSON.stringify({
            verified: false,
            error: `Payment is not completed. Status: ${stripeObject.status}`,
          }),
          { status: 400, headers: { 'Content-Type': 'application/json' } }
        )
      }

      paymentAmount = stripeObject.amount
    } else if (transactionId.startsWith('cs_')) {
      // It's a checkout session
      stripeObject = await stripe.checkout.sessions.retrieve(transactionId)

      // Verify the session was paid
      if (stripeObject.payment_status !== 'paid') {
        return new Response(
          JSON.stringify({
            verified: false,
            error: `Payment is not completed. Status: ${stripeObject.payment_status}`,
          }),
          { status: 400, headers: { 'Content-Type': 'application/json' } }
        )
      }

      paymentAmount = stripeObject.amount_total
    } else {
      // For testing purposes only
      if (transactionId.startsWith('test_') && Deno.env.get('NODE_ENV') !== 'production') {
        return new Response(
          JSON.stringify({
            verified: true,
            subscriptionType: 'MONTHLY',
            message: 'Test payment accepted. Not verified with Stripe.',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        )
      }

      return new Response(
        JSON.stringify({
          verified: false,
          error: 'Unrecognized payment ID format',
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Determine subscription type based on amount
    if (paymentAmount >= 5000) {
      // $50.00 or more (in cents)
      subscriptionType = 'YEARLY'
    } else {
      subscriptionType = 'MONTHLY'
    }

    // Prepare transaction data for database
    const now = new Date()
    const expirationDate = new Date()
    
    if (subscriptionType === 'YEARLY') {
      expirationDate.setFullYear(expirationDate.getFullYear() + 1)
    } else {
      expirationDate.setMonth(expirationDate.getMonth() + 1)
    }

    // Save verification result to database
    const { error: insertError } = await supabaseClient.from('transactions').insert({
      id: transactionId,
      user_id: userId,
      subscription_type: subscriptionType,
      purchase_date: now.toISOString(),
      expiration_date: expirationDate.toISOString(),
      status: 'verified',
      verification_method: 'supabase-edge-function',
      device_info: req.headers.get('User-Agent') || '',
      app_version: req.headers.get('X-App-Version') || '',
    })

    if (insertError) {
      console.error('Error inserting transaction:', insertError)
      return new Response(
        JSON.stringify({
          error: 'Error saving verification data',
        }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Update user's premium status
    const { error: updateUserError } = await supabaseClient
      .from('users')
      .update({
        is_premium: true,
        subscription_type: subscriptionType,
        subscription_expires: expirationDate.toISOString(),
        updated_at: now.toISOString(),
        transaction_id: transactionId,
        permanent_premium: true,
      })
      .eq('id', userId)

    if (updateUserError) {
      console.error('Error updating user:', updateUserError)
      // Don't fail the operation, just log the error
    }

    return new Response(
      JSON.stringify({
        verified: true,
        subscriptionType: subscriptionType,
        message: 'Payment successfully verified with Stripe',
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Stripe verification error:', error)

    // Handle different Stripe errors appropriately
    if (error.type === 'StripeInvalidRequestError') {
      return new Response(
        JSON.stringify({
          verified: false,
          error: 'Invalid payment ID. The transaction could not be found.',
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({
        verified: false,
        error: `Verification failed: ${error.message}`,
      }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
}) 