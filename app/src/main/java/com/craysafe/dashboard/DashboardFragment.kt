package com.craysafe.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.craysafe.LoginActivity
import com.craysafe.R
import com.craysafe.databinding.FragmentDashboardBinding
import com.craysafe.utils.SessionManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: TankDashboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        viewModel.loadDashboard(sessionManager)
    }

    private fun setupRecyclerView() {
        adapter = TankDashboardAdapter { tankId ->
            android.widget.Toast.makeText(
                requireContext(),
                "Tank $tankId clicked",
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
                binding.tvEmptyState.visibility = View.GONE
                binding.rvTanks.visibility = View.VISIBLE
            } else {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvTanks.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Optional: show progress
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            android.widget.Toast.makeText(
                requireContext(),
                error,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        sessionManager.logout()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
        android.widget.Toast.makeText(
            requireContext(),
            "Logged out successfully",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}