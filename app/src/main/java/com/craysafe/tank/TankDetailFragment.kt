// TankDetailFragment.kt
package com.craysafe.tank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.craysafe.databinding.FragmentTankDetailBinding
import com.craysafe.utils.SessionManager

class TankDetailFragment : Fragment() {

    // View Binding: type-safe access to UI elements
    private var _binding: FragmentTankDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TankDetailViewModel
    private lateinit var sessionManager: SessionManager

    // The TankID passed from Dashboard
    private var tankId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get TankID from arguments (passed from Dashboard)
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

        // Load data
        if (tankId != -1) {
            viewModel.loadTankDetail(tankId, sessionManager)
        } else {
            Toast.makeText(requireContext(), "No tank ID received", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Observe tank data changes
        viewModel.data.observe(viewLifecycleOwner) { tankData ->
            tankData?.let { updateUI(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        // Switch mode result
        viewModel.switchResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun updateUI(data: com.craysafe.api.models.TankDetailData) {
        binding.tvTankName.text = data.Tankname ?: "Tank #${data.TankID}"
        binding.tvMode.text = "Mode: ${data.Mode ?: "Growing"}"
        binding.tvTemperature.text = "${data.Temperature ?: 0.0}°C"
        binding.tvPh.text = "${data.Ph_Level ?: 0.0}"
        binding.tvTurbidity.text = "${data.Turbidity ?: 0.0} NTU"
        binding.tvStatus.text = "Status: ${data.Status ?: "Unknown"}"
        binding.tvTempTTD.text = "\uD83C\uDF21\uFE0F Temperature: ${formatTTD(data.TemperatureTTD)}"
        binding.tvPhTTD.text = "\uD83E\uDDEA pH: ${formatTTD(data.PhTTD)}"
        binding.tvTurbidityTTD.text = "\uD83D\uDCA7 Turbidity: ${formatTTD(data.TurbidityTTD)}"
        binding.tvLastUpdated.text = "Last Updated: ${data.LastUpdated ?: "N/A"}"

        // Status color
        val colorRes = when (data.Status?.lowercase()) {
            "safe" -> android.R.color.holo_green_dark
            "warning" -> android.R.color.holo_orange_dark
            "critical" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        binding.tvStatus.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
        )
    }

    // format minutes into a readable string
    private fun formatTTD(minutes: Long?): String {
        if (minutes == null) return "Safe"
        return when {
            minutes < 60 -> "⚠ ${minutes} min"
            minutes < 24 * 60 -> "⚠ ${minutes / 60} hr"
            else -> "⚠ ${minutes / (60 * 24)} days"
        }
    }

    private fun setupListeners() {
        binding.btnSwitchMode.setOnClickListener {
            showModeConfirmationDialog()
        }
    }

    private fun showModeConfirmationDialog() {
        val currentData = viewModel.data.value ?: return
        val currentMode = currentData.Mode ?: "Growing"
        val newMode = if (currentMode == "Growing") "Breeding" else "Growing"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Switch Mode?")
            .setMessage("Change from $currentMode to $newMode?\n\nThe safety thresholds will be updated for the new mode.")
            .setPositiveButton("Switch") { _, _ ->
                viewModel.switchMode(tankId, newMode, sessionManager)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}