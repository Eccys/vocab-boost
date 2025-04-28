# Firebase Function for Stripe Verification

This function securely verifies Stripe payments using the Stripe API. It prevents users from entering fake transaction IDs by checking with Stripe directly.

## Setup Instructions

### 1. Install Dependencies

Make sure you have the Firebase CLI installed:

```bash
npm install -g firebase-tools
```

Then install the function dependencies:

```bash
cd functions
npm install
```

### 2. Set up Stripe Secret Key

You need to configure your Stripe secret key for the function to work:

```bash
firebase functions:config:set stripe.secret_key=sk_test_your_secret_key_here
```

**IMPORTANT**: Never commit your Stripe secret key to version control or include it in client-side code. The Firebase Cloud Function keeps it secure.

### 3. Deploy the Function

Deploy the function to Firebase:

```bash
firebase deploy --only functions
```

## How it Works

1. When a user enters a Stripe transaction ID (pi_ or cs_ prefix), the app securely calls this function
2. The function checks if the payment exists and is successful using the Stripe API
3. It validates that the payment hasn't been used by another user
4. If valid, it returns subscription details to the app

## Testing

For testing in development, you can use test IDs that start with `test_`. These will be accepted without verification.

In production, only real Stripe payment IDs will work.

## Required Dependencies

In your functions/package.json, make sure you have:

```json
"dependencies": {
  "firebase-admin": "^11.0.0",
  "firebase-functions": "^4.0.0",
  "stripe": "^12.0.0"
}
```

## Firestore Security Rules

Add these security rules to your Firestore to protect verification data:

```
service cloud.firestore {
  match /databases/{database}/documents {
    match /transactions/{transactionId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow write: if false; // Only writable by Cloud Functions
    }
    
    match /payment_verifications/{id} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow write: if false; // Only writable by Cloud Functions
    }
  }
}
``` 