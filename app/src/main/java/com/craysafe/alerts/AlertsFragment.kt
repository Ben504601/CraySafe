package com.craysafe.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.craysafe.databinding.FragmentAlertsBinding
class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceSate: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceSate: Bundle?) {
        super.onViewCreated(view, savedInstanceSate)

        // TODO: Load alerts from API
        // For now, the Layout shows "No alerts yet" -bengie
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}