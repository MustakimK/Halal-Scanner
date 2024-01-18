package com.example.halalscanner.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.halalscanner.R
import com.squareup.picasso.Picasso

class HistoryAdapter(private val historyDataList: List<HistoryData>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val nameView: TextView = view.findViewById(R.id.nameView)
        val statusView: TextView = view.findViewById(R.id.statusView)
        val statusImageView: ImageView = view.findViewById(R.id.statusImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyData = historyDataList[position]
        holder.nameView.text = historyData.name
        holder.statusView.text = historyData.status

        if (historyData.image != "") {
            Picasso.get().load(historyData.image).into(holder.imageView)
        }

        if (holder.statusView.text == "Halal") {
            holder.statusImageView.setImageResource(R.drawable.halal)
        } else if (holder.statusView.text == "Haram") {
            holder.statusImageView.setImageResource(R.drawable.haram)
        }
        else{
            holder.statusImageView.setImageResource(R.drawable.unknown)
        }
    }

    override fun getItemCount() = historyDataList.size
}
