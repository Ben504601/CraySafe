package com.craysafe.support

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.R
import com.craysafe.api.models.DiagnosticItem
import com.craysafe.databinding.ItemDiagnosticBinding

class DiagnosticAdapter :
    RecyclerView.Adapter<DiagnosticAdapter.DiagnosticViewHolder>() {

    private var items: List<DiagnosticItem> = emptyList()
    // Track which items are expanded by qa_id
    private val expandedIds = mutableSetOf<Int>()

    fun submitList(newItems: List<DiagnosticItem>) {
        items = newItems
        expandedIds.clear()   // collapse all on refresh
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosticViewHolder {
        val binding = ItemDiagnosticBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiagnosticViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiagnosticViewHolder, position: Int) {
        val item = items[position]
        val isExpanded = expandedIds.contains(item.qa_id)
        holder.bind(item, isExpanded) {
            if (isExpanded) expandedIds.remove(item.qa_id) else expandedIds.add(item.qa_id)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size

    class DiagnosticViewHolder(
        private val binding: ItemDiagnosticBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DiagnosticItem, expanded: Boolean, onToggle: () -> Unit) {
            binding.apply {
                tvCategory.text = item.category
                tvIssueTitle.text = item.issue_title
                tvSolution.text = item.solution

                // Category-specific color
                val colorRes = when (item.category.lowercase()) {
                    "hardware" -> android.R.color.holo_orange_dark
                    "software" -> android.R.color.holo_blue_dark
                    "crayfish health" -> android.R.color.holo_green_dark
                    else -> R.color.text_tertiary
                }
                tvCategory.setTextColor(
                    ContextCompat.getColor(root.context, colorRes)
                )

                // Solution visibility
                tvSolution.visibility = if (expanded) View.VISIBLE else View.GONE
                tvHint.text = if (expanded) "Tap to collapse" else "Tap to expand"

                root.setOnClickListener { onToggle() }
            }
        }
    }
}