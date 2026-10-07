package com.craysafe.alerts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.R
import com.craysafe.api.models.Alert
import com.craysafe.databinding.ItemAlertBinding

class AlertAdapter(
    private val onAlertClick: (Alert) -> Unit
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
        return AlertViewHolder(binding, onAlertClick)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AlertViewHolder(
        private val binding: ItemAlertBinding,
        private val onAlertClick: (Alert) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: Alert) {
            val context = binding.root.context

            // Parse severity and parameter
            val typeParts = alert.alert_type.split(":", limit = 2)
            val severity = alert.severity ?: typeParts.getOrNull(0) ?: "Alert"
            val parameter = alert.parameter ?: typeParts.getOrNull(1) ?: ""

            // Parse headline from message if backend didn't provide one
            val headline = alert.headline
                ?: alert.message.split("\n").firstOrNull()?.trim()
                ?: ""

            // Colors per severity
            val (bgRes, textRes) = when (severity.lowercase()) {
                "critical" -> R.color.critical to R.color.white
                "warning" -> R.color.warning to R.color.white
                "time-to-danger" -> R.color.info to R.color.white
                else -> R.color.text_tertiary to R.color.white
            }

            val badgeBg = ContextCompat.getColor(context, bgRes)
            val badgeText = ContextCompat.getColor(context, textRes)

            binding.apply {
                // Severity badge
                tvSeverity.text = severity.uppercase()
                tvSeverity.setBackgroundColor(badgeBg)
                tvSeverity.setTextColor(badgeText)

                // Parameter as main title
                tvParameter.text = parameter.ifEmpty { severity }

                // Headline
                tvHeadline.text = headline

                // Timestamp
                tvDate.text = formatRelativeDate(alert.alert_date)

                // Left severity strip
                severityStrip.setBackgroundColor(badgeBg)

                // Unread indicator
                unreadRow.visibility =
                    if (alert.status == "unread") View.VISIBLE else View.GONE

                // Card tint: unread cards stand out slightly
                root.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        if (alert.status == "unread") R.color.surface else R.color.background
                    )
                )

                // Tap → open detail
                root.setOnClickListener { onAlertClick(alert) }
            }
        }

        private fun formatRelativeDate(raw: String): String {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(raw) ?: return raw

                val diffMin = (System.currentTimeMillis() - parsed.time) / 60_000
                when {
                    diffMin < 1 -> "just now"
                    diffMin < 60 -> "${diffMin}m ago"
                    diffMin < 24 * 60 -> "${diffMin / 60}h ago"
                    diffMin < 48 * 60 -> "yesterday"
                    else -> "${diffMin / (24 * 60)}d ago"
                }
            } catch (_: Exception) {
                raw
            }
        }
    }
}