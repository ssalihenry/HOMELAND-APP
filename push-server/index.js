const admin = require('firebase-admin');

// 1. Load the Firebase Service Account from GitHub Secrets
let serviceAccount;
try {
  // We decode the Base64 string we saved in GitHub Secrets
  const decoded = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT, 'base64').toString('utf8');
  serviceAccount = JSON.parse(decoded);
} catch (error) {
  console.error("Error parsing Firebase Service Account. Make sure the secret is a valid Base64 encoded JSON.");
  process.exit(1);
}

// 2. Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkAndSendNotifications() {
  try {
    // 3. Look for pending notifications in the database
    const snapshot = await db.collection('push_notifications')
                            .where('status', '==', 'pending')
                            .orderBy('timestamp', 'asc')
                            .limit(1)
                            .get();

    if (snapshot.empty) {
      console.log("No pending notifications found. Exiting.");
      return;
    }

    // 4. Grab the first pending notification
    const doc = snapshot.docs[0];
    const data = doc.data();
    
    console.log(`Sending notification: "${data.title}" to topic 'homeland_updates'...`);

    // 5. Send the notification via Firebase Cloud Messaging
    const message = {
      notification: {
        title: data.title,
        body: data.message,
      },
      topic: 'homeland_updates' // The Android app is subscribed to this topic
    };

    const response = await admin.messaging().send(message);
    console.log('Successfully sent message:', response);

    // 6. Mark the notification as sent so we don't send it again
    await doc.ref.update({ status: 'sent' });
    console.log("Notification marked as 'sent' in database.");

  } catch (error) {
    console.error("Error sending notification:", error);
  }
}

// Run the function
checkAndSendNotifications();
