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
import android.widget.Toast

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
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Log out?")
            .setMessage("You'll need to sign in again to monitor your tanks.")
            .setPositiveButton("Log out") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        sessionManager.logout()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        adapter = TankDashboardAdapter { tankId ->
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
            adapter = this@DashboardFragment.adapter  // ✅ Correct assignment
        }
    }

    private fun setupObservers() {
        viewModel.dashboardData.observe(viewLifecycleOwner) { data ->
            if (data.isNotEmpty()) {
                adapter.submitList(data)  // ✅ Now works
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvTanks.visibility = View.VISIBLE
            } else {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvTanks.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            android.widget.Toast.makeText(
                requireContext(),
                error,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}