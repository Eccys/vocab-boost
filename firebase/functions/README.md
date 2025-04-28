# Vocab App Firebase Functions

This directory contains the Firebase Cloud Functions for handling Stripe payment webhooks in the Vocab app.

## Setup Instructions

1. **Install Firebase CLI** (if you haven't already)
   ```
   npm install -g firebase-tools
   ```

2. **Login to Firebase**
   ```
   firebase login
   ```

3. **Initialize your Firebase project**
   ```
   firebase init
   ```
   - Select "Functions" when prompted
   - Select your Firebase project or create a new one
   - Choose "JavaScript" for the language
   - Say "No" to ESLint
   - Say "Yes" to installing dependencies

4. **Set environment variables**
   ```
   cd functions
   firebase functions:config:set stripe.secret_key="sk_test_your_stripe_secret_key" stripe.webhook_secret="whsec_your_webhook_secret"
   ```

   Get these values from:
   - Stripe Secret Key: Stripe Dashboard → Developers → API keys
   - Webhook Secret: Stripe Dashboard → Developers → Webhooks → Select your endpoint → Signing secret

5. **Deploy the function**
   ```
   firebase deploy --only functions
   ```

6. **Configure Stripe Webhook**
   - Go to Stripe Dashboard → Developers → Webhooks
   - Add Endpoint
   - Enter your function URL (shown after deployment): `https://us-central1-your-project-id.cloudfunctions.net/stripeWebhook`
   - Select events: `checkout.session.completed`
   - Click "Add endpoint"

## How It Works

1. User makes payment via Stripe Checkout
2. Stripe sends webhook to your Firebase Function
3. Function verifies the payment and stores it in Firestore
4. App checks for pending transactions on startup or resume
5. App activates premium if valid transaction is found

## Local Testing

```
cd functions
npm run serve
```

Use tools like Stripe CLI or ngrok to forward webhook events to your local machine for testing. 