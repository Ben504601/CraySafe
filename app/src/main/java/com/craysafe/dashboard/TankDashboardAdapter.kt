package com.craysafe.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.api.models.DashboardData
import com.craysafe.databinding.ItemTankDashboardBinding

class TankDashboardAdapter(
    private val onTankClick: (Int) -> Unit  // ✅ Constructor with lambda parameter
) : RecyclerView.Adapter<TankDashboardAdapter.TankViewHolder>() {

    private var items: List<DashboardData> = emptyList()

    // ✅ This method updates the list
    fun submitList(newItems: List<DashboardData>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TankViewHolder {
        val binding = ItemTankDashboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TankViewHolder(binding, onTankClick)
    }

    override fun onBindViewHolder(holder: TankViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TankViewHolder(
        private val binding: ItemTankDashboardBinding,
        private val onTankClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: DashboardData) {
            binding.apply {
                tvTankName.text = data.Tankname ?: "Tank #${data.TankID}"
                tvTemperature.text = "${data.Temperature}°C"
                tvPh.text = "${data.Ph_Level}"
                tvTurbidity.text = "${data.Turbidity} NTU"
                tvMode.text = data.Mode ?: "Growing"

                // Status color
                val statusColor = when (data.Status?.lowercase()) {
                    "safe" -> android.R.color.holo_green_dark
                    "warning" -> android.R.color.holo_orange_dark
                    "critical" -> android.R.color.holo_red_dark
                    else -> android.R.color.darker_gray
                }
                tvStatus.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        root.context,
                        statusColor
                    )
                )
                tvStatus.text = data.Status ?: "Unknown"

                // Click listener
                root.setOnClickListener {
                    onTankClick(data.TankID)
                }
            }
        }
    }
}