package com.craysafe.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.R
import com.craysafe.api.models.DashboardData
import com.craysafe.databinding.ItemTankDashboardBinding

class TankDashboardAdapter(
    private val onTankClick: (Int) -> Unit
) : RecyclerView.Adapter<TankDashboardAdapter.TankViewHolder>() {

    private var items: List<DashboardData> = emptyList()

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
                // ── 1. Text fields ──
                tvTankName.text = data.Tankname ?: "Tank #${data.TankID}"
                tvTemperature.text = formatReading(data.Temperature) + "°"
                tvPh.text = formatReading(data.Ph_Level)
                tvTurbidity.text = formatReading(data.Turbidity)
                tvMode.text = data.Mode ?: "Growing"

                // ── 2. Status: pick colors and label ──
                val status = data.Status?.lowercase() ?: "unknown"
                val (colorRes, label) = when (status) {
                    "safe" -> R.color.safe to "All readings normal"
                    "warning" -> R.color.warning to "Some readings need attention"
                    "critical" -> R.color.critical to "Readings out of safe range"
                    else -> R.color.text_tertiary to "No recent data"
                }

                val statusColor = ContextCompat.getColor(root.context, colorRes)

                // Status dot
                statusDot.background.setTint(statusColor)

                // Status text
                tvStatus.text = label
                tvStatus.setTextColor(statusColor)

                // ── 3. Mode pill background ──
                val modeBgRes = if (data.Mode == "Breeding") R.color.warning_bg else R.color.info_bg
                tvMode.setBackgroundColor(ContextCompat.getColor(root.context, modeBgRes))

                // ── 4. Click handler ──
                root.setOnClickListener {
                    onTankClick(data.TankID)
                }
            }
        }

        /**
         * Format sensor readings nicely:
         *  - null → "—" (em-dash)
         *  - 25.5 → "25.5"
         *  - 15.0 → "15"
         */
        private fun formatReading(value: Double?): String {
            if (value == null) return "—"
            return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        }
    }
}