package com.craysafe.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.craysafe.databinding.FragmentAlertsBinding
import com.craysafe.utils.SessionManager
import com.craysafe.R

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlertsViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: AlertAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val unreadOnly = checkedIds.firstOrNull() == R.id.chipUnread
            viewModel.setFilter(unreadOnly)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[AlertsViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupFilters()

        binding.errorStateLayout.btnRetry.setOnClickListener {
            viewModel.loadAlerts(sessionManager)
        }

        viewModel.loadAlerts(sessionManager)
    }

    private fun setupRecyclerView() {
        adapter = AlertAdapter { alert ->
            // Mark as read when opened
            viewModel.markAsRead(alert.alert_id, sessionManager)

            // Navigate to detail, passing everything the detail screen needs
            val bundle = Bundle().apply {
                putInt("alert_id", alert.alert_id)
                putString("severity", alert.severity
                    ?: alert.alert_type.split(":").getOrNull(0) ?: "Alert")
                putString("parameter", alert.parameter
                    ?: alert.alert_type.split(":").getOrNull(1) ?: "")
                putString("headline", alert.headline
                    ?: alert.message.split("\n").firstOrNull() ?: "")
                putString("advice", alert.advice ?: "")
                putString("tank_name", alert.tank_name ?: "")
                putString("alert_date", alert.alert_date)
            }
            findNavController().navigate(R.id.action_alerts_to_alertDetail, bundle)
        }

        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AlertsFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            binding.errorStateLayout.root.visibility = View.GONE

            if (alerts.isNotEmpty()) {
                adapter.submitList(alerts)
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvAlerts.visibility = View.VISIBLE
            } else {
                // If we're filtering unread only and there are none, the empty state should say "All clear"
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvAlerts.visibility = View.GONE
            }
        }

        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            binding.tvUnreadCount.text = if (count == 0) "No unread alerts" else "$count unread"
            (activity as? com.craysafe.MainActivity)?.updateAlertsBadge(count)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.errorStateLayout.root.visibility = View.GONE
                binding.rvAlerts.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorStateLayout.root.visibility = View.VISIBLE
                binding.errorStateLayout.tvErrorMessage.text = "Failed to load alerts"
                binding.errorStateLayout.tvErrorSubtext.text = error

                binding.rvAlerts.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
            } else {
                binding.errorStateLayout.root.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAlerts(sessionManager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}