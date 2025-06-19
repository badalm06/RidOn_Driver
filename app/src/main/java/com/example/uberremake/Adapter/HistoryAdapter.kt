package com.example.uberremake.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uberremake.Model.History
import com.example.uberremake.R

class HistoryAdapter(
    private val trips: List<History>,
    private val onTripClick: (History) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRiderName: TextView = itemView.findViewById(R.id.tvRiderName)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTripDistance: TextView = itemView.findViewById(R.id.tvTripDistance)
        val tvCharges: TextView = itemView.findViewById(R.id.tvCharges)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val trip = trips[position]
        holder.tvRiderName.text = "Rider: ${trip.riderName}"
        holder.tvDate.text = "Date: ${trip.tripStartTime}"
        holder.tvTripDistance.text = "Distance: ${trip.distanceText}"
        holder.tvCharges.text = "Charges: ₹${trip.price}"
        holder.itemView.setOnClickListener { onTripClick(trip) }
    }

    override fun getItemCount(): Int = trips.size
}

