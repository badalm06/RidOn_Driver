package com.example.uberremake.Services

import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import com.example.uberremake.Common
import com.example.uberremake.Model.DriverRequestReceived
import com.example.uberremake.login.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        Log.d("FCM_TOKEN", "Driver FCM token: $token")
        if (FirebaseAuth.getInstance().currentUser != null) {
            User.updateToken(this, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

       //  Optional: Show a Toast for debugging (remove in production)
//        android.os.Handler(android.os.Looper.getMainLooper()).post {
//            Toast.makeText(this, "FCM RECEIVED!", Toast.LENGTH_LONG).show()
//        }

        Log.d("FCM_DEBUG", "Message received: ${remoteMessage.data}")
        val data = remoteMessage.data

        // Extract fields directly using the keys as sent by backend
        val notiTitle = data["NOTI_TITLE"]
        val riderKey = data["RiderKey"]
        val pickupLocation = data["PickupLocation"]

        if (notiTitle == "REQUEST_DRIVER_TITLE" && riderKey != null && pickupLocation != null) {
            Log.d("EVENTBUS", "Sticky event posted: $riderKey, $pickupLocation")
            EventBus.getDefault().postSticky(
                DriverRequestReceived(riderKey, pickupLocation)
            )
        } else {
            Log.e("FCM_DEBUG", "Missing fields or NOTI_TITLE mismatch: $data")
            // Optionally show a notification for other types
            // If you want to show a notification for other messages, uncomment below:
             Common.showNotification(
                 this,
                 Random.nextInt(),
                 notiTitle ?: "Notification",
                 data["NOTI_BODY"] ?: "",
                 null
             )
        }
    }

//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//        val data = remoteMessage.data
//        if(data != null) {
//            if(data[Common.NOTI_TITLE].equals(Common.REQUEST_DRIVER_TITLE)) {
//
//                EventBus.getDefault()
//                    .postSticky(DriverRequestReceived(
//                        data[Common.RIDER_KEY]!!,
//                        data[Common.PICKUP_LOCATION]!!,
//                    ))
//            }
//            else {
//                Common.showNotification(this, Random.nextInt(),
//                    data[Common.NOTI_TITLE],
//                    data[Common.NOTI_BODY],
//                    null)
//            }
//        }
//
//    }
}