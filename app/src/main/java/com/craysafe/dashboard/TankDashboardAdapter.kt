package com.craysafe.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.R
import com.craysafe.api.models.DashboardData
import com.craysafe.databinding.ItemTankDashboardBinding

class TankDashboardAdapter(
    private val onTankClick: (Int) -> Unit
) : ListAdapter<DashboardData, TankDashboardAdapter.TankViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<DashboardData>() {
        override fun areItemsTheSame(oldItem: DashboardData, newItem: DashboardData) = oldItem.TankID == newItem.TankID
        override fun areContentsTheSame(oldItem: DashboardData, newItem: DashboardData) = oldItem == newItem
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
        holder.bind(getItem(position))
    }

    class TankViewHolder(
        private val binding: ItemTankDashboardBinding,
        private val onTankClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: DashboardData) {
            binding.apply {
                tvTankName.text = data.Tankname ?: "Tank #${data.TankID}"
                tvTemperature.text = formatReading(data.Temperature) + "°"
                tvPh.text = formatReading(data.Ph_Level)
                tvTurbidity.text = formatReading(data.Turbidity)
                tvMode.text = data.Mode ?: "Growing"

                val status = data.Status?.lowercase() ?: "unknown"
                val (colorRes, label) = when (status) {
                    "safe" -> R.color.safe to "All readings normal"
                    "warning" -> R.color.warning to "Some readings need attention"
                    "critical" -> R.color.critical to "Readings out of safe range"
                    else -> R.color.text_tertiary to "No recent data"
                }

                val statusColor = ContextCompat.getColor(root.context, colorRes)

                statusDot.background.setTint(statusColor)
                tvStatus.text = label
                tvStatus.setTextColor(statusColor)

                tvTemperature.setTextColor(getStatusColor(root.context, data.TemperatureStatus))
                tvPh.setTextColor(getStatusColor(root.context, data.PhStatus))
                tvTurbidity.setTextColor(getStatusColor(root.context, data.TurbidityStatus))

                val modeBgRes = if (data.Mode == "Breeding") R.color.warning_bg else R.color.info_bg
                tvMode.setBackgroundColor(ContextCompat.getColor(root.context, modeBgRes))

                root.setOnClickListener { onTankClick(data.TankID) }
            }
        }

        private fun getStatusColor(context: Context, status: String?): Int {
            val colorRes = when (status?.lowercase()) {
                "safe" -> R.color.safe
                "warning" -> R.color.warning
                "critical" -> R.color.critical
                else -> R.color.text_tertiary
            }
            return ContextCompat.getColor(context, colorRes)
        }

        private fun formatReading(value: Double?): String {
            if (value == null) return "—"
            return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        }
    }
}