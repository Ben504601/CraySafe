package com.craysafe.dashboard

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.craysafe.databinding.DialogAddTankBinding
import com.craysafe.utils.SessionManager

class AddTankDialog(
    private val onTankPaired: () -> Unit
) : DialogFragment() {

    private var _binding: DialogAddTankBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TankPairViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTankBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        viewModel = ViewModelProvider(this)[TankPairViewModel::class.java]
        
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnPair.isEnabled = !isLoading
            // Optional: show progress
        }

        viewModel.pairResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PairResult.Success -> {
                    Toast.makeText(requireContext(), "Tank paired successfully!", Toast.LENGTH_SHORT).show()
                    onTankPaired()
                    dismiss()
                }
                is PairResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnPair.setOnClickListener {
            val productId = binding.etProductId.text.toString().trim()
            if (productId.isEmpty()) {
                binding.etProductId.error = "Product ID required"
                return@setOnClickListener
            }
            viewModel.pairTank(productId, sessionManager)
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}