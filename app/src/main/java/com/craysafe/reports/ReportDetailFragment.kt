package com.craysafe.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.craysafe.R
import com.craysafe.api.models.ReportData
import com.craysafe.databinding.FragmentReportDetailBinding
import com.craysafe.utils.SessionManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ReportDetailFragment : Fragment() {

    private var _binding: FragmentReportDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ReportDetailViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ReadingAdapter

    private var parameter: String = "temperature"
    private var range: String = "7d"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[ReportDetailViewModel::class.java]

        parameter = arguments?.getString("parameter") ?: "temperature"
        range = arguments?.getString("range") ?: "7d"

        val (title, unit) = when (parameter) {
            "ph" -> "pH Level" to ""
            "turbidity" -> "Turbidity" to " NTU"
            else -> "Temperature" to "°C"
        }
        binding.tvParameterTitle.text = title
        adapter = ReadingAdapter(unit)

        binding.rvReadings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReadings.adapter = adapter

        setupChart()
        observe()
        viewModel.load(sessionManager, range)
    }

    private fun setupChart() {
        binding.chartDetail.apply {
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            legend.isEnabled = false
        }
    }

    private fun observe() {
        viewModel.reportData.observe(viewLifecycleOwner) { data ->
            data?.let { render(it) }
        }
    }

    private fun render(data: ReportData) {
        binding.tvTankNameDetail.text = data.tank.Tankname ?: "Tank #${data.tank.TankID}"

        val (values, color, unit) = when (parameter) {
            "ph" -> Triple(data.readings.map { it.ph }, R.color.primary, "")
            "turbidity" -> Triple(data.readings.map { it.turbidity }, R.color.warning, " NTU")
            else -> Triple(data.readings.map { it.temperature }, R.color.critical, "°C")
        }

        val labels = data.readings.map { it.timestamp.substring(5, 16) }
        val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
        val colorInt = androidx.core.content.ContextCompat.getColor(requireContext(), color)

        val set = LineDataSet(entries, "").apply {
            this.color = colorInt
            setCircleColor(colorInt)
            lineWidth = 2.5f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = colorInt
            fillAlpha = 30
        }

        binding.chartDetail.data = LineData(set)

        val step = (labels.size / 6).coerceAtLeast(1)
        val shortLabels = labels.filterIndexed { i, _ -> i % step == 0 }
        binding.chartDetail.xAxis.valueFormatter = IndexAxisValueFormatter(shortLabels)
        binding.chartDetail.xAxis.labelRotationAngle = -30f
        binding.chartDetail.xAxis.textSize = 9f
        binding.chartDetail.invalidate()

        // Stats
        val s = when (parameter) {
            "ph" -> data.summary?.ph
            "turbidity" -> data.summary?.turbidity
            else -> data.summary?.temperature
        }
        if (s != null) {
            binding.tvStatMin.text = "Min\n${s.min}$unit"
            binding.tvStatAvg.text = "Avg\n${s.avg}$unit"
            binding.tvStatMax.text = "Max\n${s.max}$unit"
        }

        // Last 20 readings, newest first
        val last20 = data.readings.takeLast(20).reversed()
        val selector: (com.craysafe.api.models.ReportReading) -> Double = when (parameter) {
            "ph" -> { r -> r.ph }
            "turbidity" -> { r -> r.turbidity }
            else -> { r -> r.temperature }
        }
        adapter.submit(last20, selector)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}