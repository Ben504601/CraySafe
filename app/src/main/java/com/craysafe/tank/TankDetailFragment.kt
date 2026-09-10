// TankDetailFragment.kt
package com.craysafe.tank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        }
    }

    private fun setupObservers() {
        // Observe tank data changes
        viewModel.tankData.observe(viewLifecycleOwner) { data ->
            data?.let {
                // Update UI with data
                binding.tvTankName.text = it.tankName
                binding.tvMode.text = it.mode
                binding.tvTemperature.text = "${it.temperature}°C"
                binding.tvPh.text = "${it.phLevel}"
                binding.tvTurbidity.text = "${it.turbidity} NTU"
                binding.tvStatus.text = it.status
                binding.tvTimeToDanger.text = it.timeToDanger ?: "No prediction yet"

                // Set status color
                val color = when (it.status.lowercase()) {
                    "safe" -> android.R.color.holo_green_dark
                    "warning" -> android.R.color.holo_orange_dark
                    "critical" -> android.R.color.holo_red_dark
                    else -> android.R.color.darker_gray
                }
                binding.tvStatus.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), color)
                )
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        // Mode Switch Button
        binding.btnSwitchMode.setOnClickListener {
            val currentMode = viewModel.tankData.value?.mode ?: return@setOnClickListener
            val newMode = if (currentMode == "Growing") "Breeding" else "Growing"

            // Show confirmation dialog or just switch
            viewModel.switchMode(tankId, newMode, sessionManager)
        }

        // Back button (in toolbar)
        // We'll handle this with navigation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}