package com.example.uberremake.Model

import android.location.Location
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object TripNotificationManager {

    // Flag to track if notification was already sent
    private var isDriverArrivedNotificationSent = false

    fun checkDriverArrivedAndNotify(
        driverLocationLat: Double,
        driverLocationLng: Double,
        pickupLatLng: android.location.Location, // or use your own LatLng class
        tripId: String
    ) {
        val distanceToPickup = FloatArray(1)
        Location.distanceBetween(
            driverLocationLat, driverLocationLng,
            pickupLatLng.latitude, pickupLatLng.longitude,
            distanceToPickup
        )

        if (distanceToPickup[0] <= 50 && !isDriverArrivedNotificationSent) {
            isDriverArrivedNotificationSent = true

            // Fetch riderId from Trips
            FirebaseDatabase.getInstance().getReference("Trips")
                .child(tripId)
                .child("rider")
                .get()
                .addOnSuccessListener { riderIdSnapshot ->
                    val riderId = riderIdSnapshot.getValue(String::class.java)
                    if (riderId != null) {
                        // Fetch riderToken from Token node
                        FirebaseDatabase.getInstance().getReference("Token")
                            .child(riderId)
                            .child("token")
                            .get()
                            .addOnSuccessListener { tokenSnapshot ->
                                val riderToken = tokenSnapshot.getValue(String::class.java)
                                if (riderToken != null) {
                                    // Call the notification helper
                                    NotificationHelper.sendDriverArrivedNotification(riderToken)
                                } else {
                                    Log.e("Notification", "Rider token not found for riderId: $riderId")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Notification", "Failed to get rider token", e)
                            }
                    } else {
                        Log.e("Notification", "Rider ID not found in trip")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Notification", "Failed to get rider ID", e)
                }
        } else if (distanceToPickup[0] > 50) {
            isDriverArrivedNotificationSent = false
        }
    }
}
