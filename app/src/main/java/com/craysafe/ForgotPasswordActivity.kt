package com.craysafe

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.craysafe.api.ApiClient
import com.craysafe.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private var isOtpSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmit.setOnClickListener {
            if (!isOtpSent) {
                sendOtp()
            } else {
                resetPassword()
            }
        }
    }

    private fun sendOtp() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.forgotPassword(email)
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true

                if (response.success) {
                    Toast.makeText(this@ForgotPasswordActivity, "Code sent to email!", Toast.LENGTH_LONG).show()
                    isOtpSent = true
                    binding.tilEmail.isEnabled = false
                    binding.layoutOtp.visibility = View.VISIBLE
                    binding.btnSubmit.text = "Reset Password"
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, response.message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this@ForgotPasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPassword() {
        val email = binding.etEmail.text.toString().trim()
        val otp = binding.etOtp.text.toString().trim()
        val pass = binding.etNewPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        if (otp.length != 6 || pass.length < 8) {
            Toast.makeText(this, "Please check your inputs", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.resetPassword(email, otp, pass, confirm)
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true

                if (response.success) {
                    Toast.makeText(this@ForgotPasswordActivity, "Password reset successfully!", Toast.LENGTH_LONG).show()
                    finish() // Go back to login
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, response.message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this@ForgotPasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}