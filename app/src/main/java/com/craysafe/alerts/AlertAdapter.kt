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
        return AlertViewHolder(binding, onAlertClick)
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
                // Parse "Critical:Temperature"
                val parts = alert.alert_type.split(":")
                val category = parts.getOrNull(0) ?: "Alert"
                val parameter = parts.getOrNull(1) ?: ""

                // Colors by category
                val (categoryColorRes, stripColorRes) = when (category) {
                    "Critical" -> R.color.critical to R.color.critical
                    "Warning" -> R.color.warning to R.color.warning
                    "Time-to-Danger" -> R.color.info to R.color.info
                    else -> R.color.text_tertiary to R.color.text_tertiary
                }
                val categoryColor = ContextCompat.getColor(root.context, categoryColorRes)

                tvCategory.text = category.uppercase()
                tvCategory.setTextColor(categoryColor)

                tvTitle.text = parameter.ifEmpty { category }

                // Message: strip the emoji prefix if present
                tvMessage.text = alert.message

                tvDate.text = formatRelativeDate(alert.alert_date)

                // Severity strip on the left edge
                severityStrip.setBackgroundColor(
                    ContextCompat.getColor(root.context, stripColorRes)
                )

                // Unread indicator
                unreadRow.visibility = if (alert.status == "unread") View.VISIBLE else View.GONE

                // Card appearance: unread cards have a slight tint
                if (alert.status == "unread") {
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.surface)
                    )
                } else {
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.background)
                    )
                }

                root.setOnClickListener {
                    onAlertClick(alert.alert_id)
                }
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