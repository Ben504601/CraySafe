package com.craysafe.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craysafe.api.models.ReportReading
import com.craysafe.databinding.ItemReportReadingBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReadingAdapter(private val unit: String) :
    RecyclerView.Adapter<ReadingAdapter.VH>() {

    private var items: List<ReportReading> = emptyList()
    private var selector: ((ReportReading) -> Double) = { it.temperature }

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val outputFormat = SimpleDateFormat("hh:mm a", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

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

        val displayTime = try {
            val date = inputFormat.parse(r.timestamp)
            if (date != null) outputFormat.format(date) else r.timestamp
        } catch (e: Exception) {
            r.timestamp
        }

        holder.binding.tvReadingTime.text = displayTime
        holder.binding.tvReadingValue.text = "${selector(r)}$unit"
    }

    override fun getItemCount() = items.size

    class VH(val binding: ItemReportReadingBinding) : RecyclerView.ViewHolder(binding.root)
}