package com.craysafe.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.api.models.Alert
import com.craysafe.databinding.ItemAlertBinding

class AlertAdapter(
    private val onAlertClick: (Int) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    private var items: List<Alert> = emptyList()

    fun submitList(newItems: List<Alert>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return  AlertViewHolder(binding, onAlertClick)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AlertViewHolder(
        private val binding: ItemAlertBinding,
        private val onAlertClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: Alert) {
            binding.apply {
                val parts =alert.alert_type.split(":")
                val category = parts.getOrNull(0) ?: ""
                val parameter = parts.getOrNull(1) ?: ""

                tvAlertType.text = when (category) {
                    "Critical" -> "\uD83D\uDD34 $parameter - Critical"
                    "Warning" -> "\uD83D\uDFE0 $parameter - Warning"
                    "Time-to-Danger" -> "\uD83D\uDFE1 $parameter - Prediction"
                    else -> alert.alert_type
                }

                tvMessage.text = alert.message
                tvDate.text = formatDate(alert.alert_date)

                val card = root as com.google.android.material.card.MaterialCardView

                if (alert.status == "unread") {
                    card.setCardBackgroundColor(0xFFF5F5F5.toInt())
                } else {
                    card.setCardBackgroundColor(0xFFFFFFFF.toInt())
                }

                root.setOnClickListener {
                    onAlertClick(alert.alert_id)
                }
            }
        }

        private fun formatDate(rawDate: String): String {
            return try {
                val formats = listOf(
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyy-MM-dd'T'HH:mm:ss'Z'"
                )
                var parsed: java.util.Date? = null
                for (fmt in formats) {
                    try {
                        val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        parsed = sdf.parse(rawDate)
                        if (parsed != null) break
                    } catch (_: Exception) {}
                }
                if (parsed == null) return rawDate

                val diffMs = System.currentTimeMillis() - parsed.time
                val diffMin = diffMs / 60_000
                when {
                    diffMin < 1 -> "just now"
                    diffMin < 60 -> "$diffMin min ago"
                    diffMin < 24 * 60 -> "${diffMin / 60} hr ago"
                    diffMin < 48 * 60 -> "yesterday"
                    diffMin < 30 * 24 * 60 -> "${diffMin / (24 * 60)} days ago"
                    else -> rawDate.substringBefore(" ")
                }
            } catch (e: Exception) {
                rawDate
            }
        }
    }
}