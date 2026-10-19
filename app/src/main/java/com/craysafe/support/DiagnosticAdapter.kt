package com.craysafe.support

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.R
import com.craysafe.api.models.DiagnosticItem
import com.craysafe.databinding.ItemDiagnosticBinding

class DiagnosticAdapter(
    private val onFeedbackClick: (qaId: Int, isHelpful: Boolean) -> Unit
) : ListAdapter<DiagnosticItem, DiagnosticAdapter.DiagnosticViewHolder>(DiffCallback()) {

    private val expandedIds = mutableSetOf<Int>()

    class DiffCallback : DiffUtil.ItemCallback<DiagnosticItem>() {
        override fun areItemsTheSame(oldItem: DiagnosticItem, newItem: DiagnosticItem) = oldItem.qa_id == newItem.qa_id
        override fun areContentsTheSame(oldItem: DiagnosticItem, newItem: DiagnosticItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosticViewHolder {
        val binding = ItemDiagnosticBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiagnosticViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiagnosticViewHolder, position: Int) {
        val item = getItem(position)
        val isExpanded = expandedIds.contains(item.qa_id)

        holder.bind(item, isExpanded, {
            if (isExpanded) expandedIds.remove(item.qa_id) else expandedIds.add(item.qa_id)
            notifyItemChanged(position)
        }, onFeedbackClick)
    }

    class DiagnosticViewHolder(
        private val binding: ItemDiagnosticBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: DiagnosticItem,
            expanded: Boolean,
            onToggle: () -> Unit,
            onFeedback: (Int, Boolean) -> Unit
        ) {
            binding.apply {
                tvCategory.text = item.category
                tvIssueTitle.text = item.issue_title
                tvSolution.text = item.solution

                val colorRes = when (item.category.lowercase()) {
                    "hardware" -> android.R.color.holo_orange_dark
                    "software" -> android.R.color.holo_blue_dark
                    "crayfish health" -> android.R.color.holo_green_dark
                    else -> R.color.text_tertiary
                }
                tvCategory.setTextColor(ContextCompat.getColor(root.context, colorRes))

                tvSolution.visibility = if (expanded) View.VISIBLE else View.GONE
                feedbackLayout.visibility = if (expanded) View.VISIBLE else View.GONE
                tvHint.text = if (expanded) "Tap to collapse" else "Tap to expand"

                // Feedback counts
                tvHelpfulCount.text = item.helpful_count.toString()
                tvUnhelpfulCount.text = item.unhelpful_count.toString()

                // Highlight active votes
                val isUpvoted = item.user_vote == 1
                val isDownvoted = item.user_vote == 0

                btnHelpful.setColorFilter(ContextCompat.getColor(root.context, if (isUpvoted) R.color.safe else R.color.text_tertiary))
                btnUnhelpful.setColorFilter(ContextCompat.getColor(root.context, if (isDownvoted) R.color.critical else R.color.text_tertiary))

                // Feedback click listeners
                btnHelpful.setOnClickListener { onFeedback(item.qa_id, true) }
                btnUnhelpful.setOnClickListener { onFeedback(item.qa_id, false) }

                // Card click listener
                root.setOnClickListener { onToggle() }
            }
        }
    }
}