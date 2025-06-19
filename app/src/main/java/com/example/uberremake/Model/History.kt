package com.example.uberremake.Model

data class History(
    val tripId: String = "",
    val driverId: String = "",
    val riderName: String = "",
    val origin: String = "",
    val destination: String = "",
    val tripStartTime: String = "",
    val status: String = "",
    val price: Double = 0.0,
    val distanceText: String = "",
    val duration: String = "",
    val start_address: String = "",
    val end_address: String = ""
    // Add other fields as needed
)



