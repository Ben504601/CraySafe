package com.craysafe.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.craysafe.LoginActivity
import com.craysafe.R
import com.craysafe.databinding.FragmentDashboardBinding
import com.craysafe.utils.SessionManager
import androidx.navigation.fragment.findNavController

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: TankDashboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddTank.setOnClickListener {
            val dialog = AddTankDialog {
                // Callback when tank is paired - refresh dashboard
                viewModel.loadDashboard(sessionManager)
            }
            dialog.show(childFragmentManager, "AddTankDialog")
        }

        binding.errorStateLayout.btnRetry.setOnClickListener {
            viewModel.loadDashboard(sessionManager)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        viewModel.loadDashboard(sessionManager)
    }

    private fun showLogoutConfirmation() {
        val dialog = LogoutDialog {
            logout()
        }
        dialog.show(childFragmentManager, "LogoutDialog")
    }

    private fun logout() {
        sessionManager.logout()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun setupRecyclerView() {
        adapter = TankDashboardAdapter { tankId ->
            sessionManager.setSelectedTankId(tankId)
            val bundle = Bundle().apply {
                putInt("tank_id", tankId)
            }
            findNavController().navigate(
                R.id.action_dashboard_to_tankDetail,
                bundle
            )
        }

        binding.rvTanks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DashboardFragment.adapter
        }
    }

    private fun setupObservers() {
        // 1. Loading State
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.errorStateLayout.root.visibility = View.GONE
                binding.rvTanks.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
            }
        }

        // 2. Error State
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorStateLayout.root.visibility = View.VISIBLE
                binding.errorStateLayout.tvErrorMessage.text = "Failed to load dashboard"
                binding.errorStateLayout.tvErrorSubtext.text = error

                binding.rvTanks.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
            } else {
                binding.errorStateLayout.root.visibility = View.GONE
            }
        }

        // 3. Success / Empty State
        viewModel.dashboardData.observe(viewLifecycleOwner) { data ->
            binding.errorStateLayout.root.visibility = View.GONE // clear any previous error

            if (data.isNotEmpty()) {
                adapter.submitList(data)
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvTanks.visibility = View.VISIBLE
            } else {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvTanks.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling(sessionManager)
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