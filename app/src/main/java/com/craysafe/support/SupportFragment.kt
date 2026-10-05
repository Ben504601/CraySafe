package com.craysafe.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.craysafe.R
import com.craysafe.databinding.FragmentSupportBinding
import com.craysafe.utils.SessionManager

class SupportFragment : Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SupportViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: DiagnosticAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[SupportViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupSearch()
        setupFilters()

        viewModel.loadQnA(sessionManager)
    }

    private fun setupRecyclerView() {
        adapter = DiagnosticAdapter()

        binding.rvDiagnostics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SupportFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                adapter.submitList(items)
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvDiagnostics.visibility = View.VISIBLE
            } else {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvDiagnostics.visibility = View.GONE
            }
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

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.filterBySearch(text?.toString() ?: "")
        }
    }

    private fun setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipHardware -> "Hardware"
                R.id.chipSoftware -> "Software"
                R.id.chipHealth -> "Crayfish Health"
                else -> null
            }
            viewModel.filterByCategory(category)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}