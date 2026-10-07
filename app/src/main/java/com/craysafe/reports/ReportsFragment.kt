package com.craysafe.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.craysafe.R
import com.craysafe.api.ApiClient
import com.craysafe.api.models.ReportData
import com.craysafe.databinding.FragmentReportsBinding
import com.craysafe.utils.SessionManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.io.File
import android.os.Environment

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ReportsViewModel
    private lateinit var sessionManager: SessionManager
    private var currentReport: ReportData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[ReportsViewModel::class.java]

        setupCharts()
        setupFilters()
        setupListeners()
        observe()

        viewModel.loadReport("7d", sessionManager)
    }

    private fun setupCharts() {
        listOf(binding.chartTemperature, binding.chartPh, binding.chartTurbidity).forEach { chart ->
            chart.apply {
                description.isEnabled = false
                setTouchEnabled(false)
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.setDrawLabels(false)
                legend.isEnabled = false
                setViewPortOffsets(20f, 10f, 20f, 10f)
            }
        }
    }

    private fun setupFilters() {
        binding.chipGroupRange.setOnCheckedStateChangeListener { _, ids ->
            val range = when (ids.firstOrNull()) {
                R.id.chip14d -> "14d"
                R.id.chip30d -> "30d"
                else -> "7d"
            }
            viewModel.loadReport(range, sessionManager)
        }
    }

    private fun setupListeners() {
        binding.cardTemperature.setOnClickListener {
            navigateToDetail("temperature")
        }
        binding.cardPh.setOnClickListener {
            navigateToDetail("ph")
        }
        binding.cardTurbidity.setOnClickListener {
            navigateToDetail("turbidity")
        }
        binding.btnDownloadPdf.setOnClickListener {
            downloadPdf()
        }
    }

    private fun navigateToDetail(parameter: String) {
        val bundle = Bundle().apply {
            putString("parameter", parameter)
            putString("range", viewModel.getCurrentRange())
        }
        findNavController().navigate(R.id.action_reports_to_detail, bundle)
    }

    private fun observe() {
        viewModel.reportData.observe(viewLifecycleOwner) { data ->
            currentReport = data
            if (data == null || data.readings.isEmpty()) {
                showEmpty()
            } else {
                showData(data)
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showData(data: ReportData) {
        binding.tvTankName.text = data.tank.Tankname ?: "Tank #${data.tank.TankID}"
        binding.contentLayout.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        // Averages
        data.summary?.let { s ->
            binding.tvTempAvg.text = "${s.temperature.avg}°C"
            binding.tvPhAvg.text = "${s.ph.avg}"
            binding.tvTurbAvg.text = "${s.turbidity.avg} NTU"
        }

        renderMiniChart(binding.chartTemperature, data.readings.map { it.temperature }, R.color.critical)
        renderMiniChart(binding.chartPh, data.readings.map { it.ph }, R.color.primary)
        renderMiniChart(binding.chartTurbidity, data.readings.map { it.turbidity }, R.color.warning)
    }

    private fun showEmpty() {
        binding.contentLayout.visibility = View.VISIBLE
        binding.chartTemperature.visibility = View.GONE
        binding.chartPh.visibility = View.GONE
        binding.chartTurbidity.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    private fun renderMiniChart(chart: com.github.mikephil.charting.charts.LineChart, values: List<Double>, colorRes: Int) {
        val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
        val color = androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)

        val set = LineDataSet(entries, "").apply {
            this.color = color
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 40
        }
        chart.data = LineData(set)
        chart.invalidate()
    }

    private fun downloadPdf() {
        val tankId = viewModel.getTankId()
        val range = viewModel.getCurrentRange()
        if (tankId == -1) {
            Toast.makeText(requireContext(), "Select a tank first", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Downloading PDF...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val body = ApiClient.apiService.downloadReportPdf("Bearer $token", tankId, range)

                val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, "craysafe_report_${tankId}_${range}.pdf")
                file.outputStream().use { out -> body.byteStream().copyTo(out) }

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(intent, "Open PDF with"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}