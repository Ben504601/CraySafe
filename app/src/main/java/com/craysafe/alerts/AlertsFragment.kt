package com.craysafe.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.craysafe.databinding.FragmentAlertsBinding
import com.craysafe.utils.SessionManager

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[AlertsViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        viewModel.loadAlerts(sessionManager)
    }

    private fun setupRecyclerView() {
        adapter = AlertAdapter { alertId ->
            viewModel.markAsRead(alertId, sessionManager)
        }

        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AlertsFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            if (alerts.isNotEmpty()) {
                adapter.submitList(alerts)
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvAlerts.visibility = View.VISIBLE
            } else {
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
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
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