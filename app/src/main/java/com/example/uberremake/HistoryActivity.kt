package com.example.uberremake

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uberremake.Adapter.HistoryAdapter
import com.example.uberremake.Model.History
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoHistory: TextView

    private val trips = mutableListOf<History>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerViewHistory)
        progressBar = findViewById(R.id.progressBar)
        tvNoHistory = findViewById(R.id.tvNoHistory)

        adapter = HistoryAdapter(trips) { trip ->
            showTripDetailsDialog(trip)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchHistory()
    }

    private fun fetchHistory() {
        progressBar.visibility = View.VISIBLE
        tvNoHistory.visibility = View.GONE
        recyclerView.visibility = View.GONE

        val driverId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val historyRef = FirebaseDatabase.getInstance().getReference("History")

        // Query for trips where driverId == current user
        historyRef.orderByChild("driverId").equalTo(driverId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trips.clear()
                    if (snapshot.exists()) {
                        for (tripSnap in snapshot.children) {
                            val trip = tripSnap.getValue(History::class.java)
                            if (trip != null) {
                                trips.add(trip.copy(tripId = tripSnap.key ?: ""))
                            }
                        }
                        trips.reverse() // latest first
                        adapter.notifyDataSetChanged()
                        recyclerView.visibility = View.VISIBLE
                        tvNoHistory.visibility = View.GONE
                    } else {
                        tvNoHistory.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                    progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    tvNoHistory.text = "Failed to load history."
                    tvNoHistory.visibility = View.VISIBLE
                }
            })
    }

    private fun showTripDetailsDialog(trip: History) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_trip_details, null)

        dialogView.findViewById<TextView>(R.id.tvDialogRider).text = "Rider: ${trip.riderName}"
        dialogView.findViewById<TextView>(R.id.tvDialogDate).text = "Date & Time: ${trip.tripStartTime}"
        dialogView.findViewById<TextView>(R.id.tvDialogStatus).text = "Status: ${trip.status}"
        dialogView.findViewById<TextView>(R.id.tvDialogCharges).text = "Charges: ₹${trip.price}"
        dialogView.findViewById<TextView>(R.id.tvDialogDistance).text = "Trip Distance: ${trip.distanceText}"
        dialogView.findViewById<TextView>(R.id.tvDialogDuration).text = "Trip Duration: ${trip.duration}"
        dialogView.findViewById<TextView>(R.id.tvDialogStart).text = "Start Address: ${trip.start_address}"
        dialogView.findViewById<TextView>(R.id.tvDialogEnd).text = "End Address: ${trip.end_address}"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

}
