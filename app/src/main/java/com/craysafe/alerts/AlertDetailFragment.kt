package com.craysafe.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.craysafe.R
import com.craysafe.databinding.FragmentAlertDetailBinding

class AlertDetailFragment : Fragment() {

    private var _binding: FragmentAlertDetailBinding? = null
    private val binding get() = _binding!!

    // Arguments passed from the list
    private var alertId: Int = -1
    private var severity: String = "Alert"
    private var parameter: String = ""
    private var headline: String = ""
    private var advice: String = ""
    private var tankName: String = ""
    private var alertDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            alertId = it.getInt("alert_id", -1)
            severity = it.getString("severity") ?: "Alert"
            parameter = it.getString("parameter") ?: ""
            headline = it.getString("headline") ?: ""
            advice = it.getString("advice") ?: ""
            tankName = it.getString("tank_name") ?: ""
            alertDate = it.getString("alert_date") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()

        // Colors per severity
        val (bgRes, _) = when (severity.lowercase()) {
            "critical" -> R.color.critical to R.color.white
            "warning" -> R.color.warning to R.color.white
            "time-to-danger" -> R.color.info to R.color.white
            else -> R.color.text_tertiary to R.color.white
        }
        val badgeBg = ContextCompat.getColor(ctx, bgRes)

        // Header
        binding.tvSeverityBadge.text = severity.uppercase()
        binding.tvSeverityBadge.setBackgroundColor(badgeBg)
        binding.tvParameter.text = parameter.ifEmpty { severity }
        binding.tvTankName.text = tankName.ifEmpty { "Tank" }
        binding.tvDate.text = formatDate(alertDate)

        // The issue
        binding.tvHeadline.text = headline.ifEmpty { "See details below" }

        // Advice
        if (advice.isBlank()) {
            binding.cardAdvice.visibility = View.GONE
            binding.labelAdvice.visibility = View.GONE
        } else {
            binding.tvAdvice.text = advice
        }

        // Set the toolbar title
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar
            ?.title = "Alert Details"
    }

    private fun formatDate(raw: String): String {
        return try {
            val input = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            input.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val parsed = input.parse(raw) ?: return raw

            val output = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.US)
            output.format(parsed)
        } catch (_: Exception) {
            raw
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}