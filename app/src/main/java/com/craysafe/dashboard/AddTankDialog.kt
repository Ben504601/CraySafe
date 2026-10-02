package com.craysafe.dashboard

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.craysafe.R
import com.craysafe.databinding.DialogAddTankBinding
import com.craysafe.utils.SessionManager

class AddTankDialog(
    private val onTankPaired: () -> Unit
) : DialogFragment() {

    private var _binding: DialogAddTankBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TankPairViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_CraySafe_Dialog)
    }

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

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPair.isEnabled = !isLoading
            binding.btnCancel.isEnabled = !isLoading
            binding.tilProductId.isEnabled = !isLoading

            // Show "Pairing..." text while loading
            binding.btnPair.text = if (isLoading) "Pairing..." else "Pair tank"
        }

        viewModel.pairResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PairResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "✅ ${result.tank?.Tankname ?: "Tank"} paired successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    onTankPaired()
                    dismiss()
                }
                is PairResult.Error -> {
                    // Show the error inline — not just as a toast
                    binding.tilProductId.error = result.message
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnPair.setOnClickListener {
            val productId = binding.etProductId.text.toString().trim().uppercase()

            // Reset any previous error
            binding.tilProductId.error = null

            if (productId.isEmpty()) {
                binding.tilProductId.error = "Please enter your Product ID"
                return@setOnClickListener
            }

            if (productId.length < 2) {
                binding.tilProductId.error = "Product ID looks too short"
                return@setOnClickListener
            }

            viewModel.pairTank(productId, sessionManager)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Clear error as the user types
        binding.etProductId.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilProductId.error = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Make the dialog occupy a reasonable width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}