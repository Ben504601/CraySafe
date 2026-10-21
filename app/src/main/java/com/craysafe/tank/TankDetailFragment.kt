package com.craysafe.tank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.craysafe.R
import com.craysafe.api.models.TankDetailData
import com.craysafe.databinding.FragmentTankDetailBinding
import com.craysafe.utils.SessionManager

class TankDetailFragment : Fragment() {

    private var _binding: FragmentTankDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TankDetailViewModel
    private lateinit var sessionManager: SessionManager
    private var tankId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tankId = it.getInt("tank_id", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTankDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[TankDetailViewModel::class.java]

        setupObservers()
        setupListeners()

        if (tankId != -1) {
            viewModel.loadTankDetail(tankId, sessionManager)
        } else {
            Toast.makeText(requireContext(), "No tank selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.data.observe(viewLifecycleOwner) { data ->
            data?.let { updateUI(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSwitchMode.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.switchResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.renameResult.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(requireContext(), "Tank renamed", Toast.LENGTH_SHORT).show()
                viewModel.resetRenameState()
            } else if (success == false) {
                Toast.makeText(requireContext(), "Failed to rename tank", Toast.LENGTH_SHORT).show()
                viewModel.resetRenameState()
            }
        }
    }

    private fun updateUI(data: TankDetailData) {
        // Header
        binding.tvTankName.text = data.Tankname ?: "Tank #${data.TankID}"
        binding.tvMode.text = "${data.Mode ?: "Growing"} mode"

        // Readings — use em-dash for missing
        binding.tvTemperature.text = formatReading(data.Temperature, "°")
        binding.tvPh.text = formatReading(data.Ph_Level, "")
        binding.tvTurbidity.text = formatReading(data.Turbidity, "")

        // Per-parameter status (backend truth) + TTD as secondary info
        binding.tvTempTTD.text = formatParamLabel(data.TemperatureStatus, data.TemperatureTTD)
        binding.tvPhTTD.text   = formatParamLabel(data.PhStatus, data.PhTTD)
        binding.tvTurbidityTTD.text = formatParamLabel(data.TurbidityStatus, data.TurbidityTTD)

        // Color-code by current status, not TTD
        binding.tvTempTTD.setTextColor(statusColor(data.TemperatureStatus))
        binding.tvPhTTD.setTextColor(statusColor(data.PhStatus))
        binding.tvTurbidityTTD.setTextColor(statusColor(data.TurbidityStatus))

        // Status
        binding.tvStatus.text = "Status: ${data.Status ?: "Unknown"}"
        binding.tvStatus.setTextColor(statusColor(data.Status))

        // Last updated
        binding.tvLastUpdated.text = "Updated ${formatTimestamp(data.LastUpdated)}"

        // Mode pill background — color depends on mode
        val pillBg = if (data.Mode == "Breeding") R.color.warning_bg else R.color.info_bg
        binding.tvMode.setBackgroundColor(ContextCompat.getColor(requireContext(), pillBg))
    }

    private fun setupListeners() {
        binding.btnSwitchMode.setOnClickListener {
            val current = viewModel.data.value?.Mode ?: "Growing"
            val next = if (current == "Growing") "Breeding" else "Growing"
            confirmModeSwitch(current, next)
        }

        binding.btnEditName.setOnClickListener {
            val currentName = viewModel.data.value?.Tankname ?: ""
            showRenameDialog(currentName)
        }
    }

    private fun confirmModeSwitch(current: String, next: String) {
        AlertDialog.Builder(requireContext(), R.style.Theme_CraySafe_Dialog)
            .setTitle("Switch to $next mode?")
            .setMessage(
                "The safety thresholds will change. Current mode: $current.\n\n" +
                        "Readings that were previously safe might become warnings (and vice versa)."
            )
            .setPositiveButton("Switch") { _, _ ->
                viewModel.switchMode(tankId, next, sessionManager)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(currentName: String) {
        val dialogBinding = com.craysafe.databinding.DialogRenameTankBinding.inflate(layoutInflater)
        dialogBinding.etTankName.setText(currentName)

        // Move cursor to the end of the text
        dialogBinding.etTankName.setSelection(currentName.length)

        AlertDialog.Builder(requireContext(), R.style.Theme_CraySafe_Dialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newName = dialogBinding.etTankName.text.toString().trim()
                if (newName.isNotEmpty() && newName != currentName) {
                    viewModel.renameTank(tankId, newName, sessionManager)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Formatting helpers ───

    private fun formatReading(value: Double?, suffix: String): String {
        if (value == null) return "—"
        val num = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        return "$num$suffix"
    }

    /**
     * Build the per-parameter label shown on the right side of each row.
     * Format:  {Status}                          e.g. "Critical"
     *          {Status} • {TTD}                  e.g. "Warning • in 2h"
     *
     * TTD is only appended when a breach is predicted within 7 days,
     * so the row stays clean during normal operation.
     */
    private fun formatParamLabel(status: String?, ttd: Long?): String {
        val base = status ?: "Unknown"
        if (ttd == null || ttd >= 7L * 24 * 60) return base
        return "$base • ${formatTTDShort(ttd)}"
    }

    private fun formatTTDShort(minutes: Long): String = when {
        minutes < 60 -> "in ${minutes}m"
        minutes < 24 * 60 -> "in ${minutes / 60}h"
        else -> "in ${minutes / (24 * 60)}d"
    }

    private fun statusColor(status: String?): Int {
        val res = when (status?.lowercase()) {
            "safe" -> R.color.safe
            "warning" -> R.color.warning
            "critical" -> R.color.critical
            else -> R.color.text_secondary
        }
        return ContextCompat.getColor(requireContext(), res)
    }

    private fun formatTimestamp(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val parsed = sdf.parse(raw) ?: return raw

            val diffMin = (System.currentTimeMillis() - parsed.time) / 60_000
            when {
                diffMin < 1 -> "just now"
                diffMin < 60 -> "${diffMin}m ago"
                diffMin < 24 * 60 -> "${diffMin / 60}h ago"
                else -> "${diffMin / (24 * 60)}d ago"
            }
        } catch (_: Exception) {
            raw
        }
    }

    override fun onResume() {
        super.onResume()
        if (tankId != -1) {
            viewModel.startPolling(tankId, sessionManager)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}