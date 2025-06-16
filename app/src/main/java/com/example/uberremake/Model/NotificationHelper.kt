package com.example.uberremake.Model

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

object NotificationHelper {
    private val client = OkHttpClient()

    fun sendDriverArrivedNotification(riderToken: String) {
        val title = "Driver Arrived"
        val body = "Your driver has arrived!"
        val url = "https://uber-notifications.el.r.appspot.com/sendNotification"

        // Build a JSON object as a string
        val json = """{
        "riderToken": "$riderToken",
        "title": "$title",
        "body": "$body"
    }"""

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotificationHelper", "Failed to send notification", e)
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("NotificationHelper", "Notification sent: ${response.body?.string()}")
            }
        })
    }

    fun sendTripCompletedNotification(riderToken: String) {
        val title = "Trip Completed"
        val body = "Your trip has been completed!"
        val url = "https://uber-notifications.el.r.appspot.com/sendNotification"

        val json = """{
        "riderToken": "$riderToken",
        "title": "$title",
        "body": "$body"
    }"""

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotificationHelper", "Failed to send trip completed notification", e)
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("NotificationHelper", "Trip completed notification sent: ${response.body?.string()}")
            }
        })
    }

}
