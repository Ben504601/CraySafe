package com.craysafe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.craysafe.auth.LoginResult
import com.craysafe.auth.RegisterViewModel
import com.craysafe.databinding.ActivityRegisterBinding
import com.craysafe.utils.SessionManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }

        // Observe registration result
        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> {
                    result.user?.let { user ->
                        sessionManager.saveAuth(
                            token = result.token,
                            userId = user.id,
                            username = user.username,
                            email = user.email,
                            role = user.role
                        )
                    }
                    Toast.makeText(this, "✅ Registration successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginResult.Error -> {
                    Toast.makeText(this, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Register button click
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (username.isEmpty()) {
                binding.etUsername.error = "Username is required"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            viewModel.register(username, email, password, confirmPassword)
        }

        // Login link click
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}