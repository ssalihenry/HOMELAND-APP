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
    // 3. Look for pending notifications (Removed .orderBy to avoid Firestore index errors)
    const snapshot = await db.collection('push_notifications')
                            .where('status', '==', 'pending')
                            .limit(20) // Process up to 20 at a time
                            .get();

    if (snapshot.empty) {
      console.log("No pending notifications found. Exiting.");
      return;
    }

    console.log(`Found ${snapshot.size} pending notification(s).`);

    // 4. Loop through pending notifications
    for (const doc of snapshot.docs) {
      const data = doc.data();
      console.log(`Sending notification: "${data.title}" to topic 'homeland_updates'...`);

      try {
        // 5. Send the notification via FCM
        const message = {
          notification: {
            title: data.title || 'Homeland',
            body: data.message || 'New update available!',
          },
          topic: 'homeland_updates', // The Android app is subscribed to this topic
          android: {
            priority: 'high',
            notification: {
              channelId: 'homeland_default_channel', // MUST match your Kotlin service
              sound: 'default'
            }
          }
        };

        const response = await admin.messaging().send(message);
        console.log(`✅ Successfully sent doc=${doc.id}:`, response);

        // 6. Mark as sent
        await doc.ref.update({ status: 'sent' });
      } catch (error) {
        // If a specific message fails, mark it as failed so it doesn't block the queue forever
        console.error(`❌ Error sending doc=${doc.id}:`, error.message);
        await doc.ref.update({ status: 'failed', error: error.message });
      }
    }
    console.log("Run complete.");
  } catch (error) {
    console.error("Fatal error during queue check:", error);
  }
}

// Run the function
checkAndSendNotifications();
