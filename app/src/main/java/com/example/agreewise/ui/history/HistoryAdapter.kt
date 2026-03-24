package com.example.agreewise.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.agreewise.R
import com.example.agreewise.databinding.ItemHistoryBinding
import com.example.agreewise.model.HistoryItem

class HistoryAdapter(
    private val items: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textTitle.text = item.title
        holder.binding.textDate.text = item.date
        holder.binding.textRiskLevel.text = item.riskLevel

        val color = when (item.riskLevel.uppercase()) {
            "HIGH RISK!" -> holder.itemView.context.getColor(R.color.risk_high)
            "MEDIUM RISK!" -> holder.itemView.context.getColor(R.color.risk_medium)
            else -> holder.itemView.context.getColor(R.color.risk_low)
        }

        holder.binding.textRiskLevel.setTextColor(color)
        holder.binding.iconRisk.setColorFilter(color)
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}