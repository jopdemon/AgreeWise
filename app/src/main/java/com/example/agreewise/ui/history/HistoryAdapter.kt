package com.example.agreewise.ui.history

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.agreewise.R
import com.example.agreewise.databinding.ItemHistoryBinding
import com.example.agreewise.model.HistoryItem

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onDeleteClick: (HistoryItem) -> Unit,
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

        val color = when {
            item.riskLevel.contains("High", ignoreCase = true) -> holder.itemView.context.getColor(R.color.risk_high)
            item.riskLevel.contains("Medium", ignoreCase = true) -> holder.itemView.context.getColor(R.color.risk_medium)
            else -> holder.itemView.context.getColor(R.color.risk_low)
        }

        // Apply dynamic colors to text and icons
        holder.binding.textRiskLevel.setTextColor(color)
        holder.binding.iconRisk.setColorFilter(color)
        
        // Remove background color from textRiskLevel if it's blocking the icon
        holder.binding.textRiskLevel.background = null
        
        // Dynamic Warning Icon
        if (item.riskLevel.contains("Low", ignoreCase = true) || item.riskLevel.contains("Safe", ignoreCase = true)) {
            holder.binding.iconRisk.setImageResource(R.drawable.ic_check_circle)
        } else {
            holder.binding.iconRisk.setImageResource(android.R.drawable.ic_dialog_alert)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
        
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<HistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }
}