package com.example.uberremake.Model

data class DriverRequestReceived(
    var tripId: String,
    var pickupLocation: String,
    var destinationLocation: String // Add this field
)
