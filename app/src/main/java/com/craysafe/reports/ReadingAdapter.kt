package com.craysafe.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.api.models.ReportReading
import com.craysafe.databinding.ItemReportReadingBinding

class ReadingAdapter(private val unit: String) :
    RecyclerView.Adapter<ReadingAdapter.VH>() {

    private var items: List<ReportReading> = emptyList()
    private var selector: ((ReportReading) -> Double) = { it.temperature }

    fun submit(list: List<ReportReading>, selector: (ReportReading) -> Double) {
        this.items = list
        this.selector = selector
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemReportReadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.binding.tvReadingTime.text = r.timestamp
        holder.binding.tvReadingValue.text = "${selector(r)}$unit"
    }

    override fun getItemCount() = items.size

    class VH(val binding: ItemReportReadingBinding) : RecyclerView.ViewHolder(binding.root)
}