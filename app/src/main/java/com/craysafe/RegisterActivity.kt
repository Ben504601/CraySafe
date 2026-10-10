package com.craysafe

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
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

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility =
                if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.tilUsername.isEnabled = !isLoading
            binding.tilEmail.isEnabled = !isLoading
            binding.tilPassword.isEnabled = !isLoading
            binding.tilConfirmPassword.isEnabled = !isLoading
            binding.tilProductId.isEnabled = !isLoading
        }

        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> onRegisterSuccess(result)
                is LoginResult.Error -> {
                    Toast.makeText(this, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun onRegisterSuccess(result: LoginResult.Success) {
        result.user?.let { user ->
            sessionManager.saveAuth(
                token = result.token,
                userId = user.id,
                username = user.username,
                email = user.email,
                role = user.role
            )
        }
        Toast.makeText(this, "Account created. Welcome!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            hideKeyboard()

            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()
            val productId = binding.etProductId.text.toString().trim()

            // Clear previous errors
            binding.tilUsername.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null
            binding.tilProductId.error = null

            // Validate username
            if (username.isEmpty()) {
                binding.tilUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (username.length < 3) {
                binding.tilUsername.error = "At least 3 characters"
                return@setOnClickListener
            }

            // Validate email
            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            // Validate password strength
            val passwordError = validatePassword(password, username, email)
            if (passwordError != null) {
                binding.tilPassword.error = passwordError
                return@setOnClickListener
            }

            // Validate confirmation
            if (password != confirm) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Validate product ID
            if (productId.isEmpty()) {
                binding.tilProductId.error = "Product ID is required"
                return@setOnClickListener
            }

            viewModel.register(username, email, password, confirm, productId)
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validatePassword(password: String, username: String, email: String): String? {
        if (password.isEmpty()) {
            return "Password is required"
        }

        if (password.length < 8) {
            return "Password must be at least 8 characters"
        }

        if (!password.any { it.isLowerCase() }) {
            return "Password must include a lowercase letter"
        }

        if (!password.any { it.isUpperCase() }) {
            return "Password must include an uppercase letter"
        }

        if (!password.any { it.isDigit() }) {
            return "Password must include a number"
        }

        val commonPasswords = setOf(
            "password", "password1",
            "password123", "12345678"
        )
        if (password.lowercase() in commonPasswords) {
            return "That password is too common - please choose another"
        }

        val lowerUsername = username.lowercase()
        val emailPrefix = email.substringBefore("@").lowercase()
        val lowerPassword = password.lowercase()
        if (lowerUsername.isNotEmpty() && lowerUsername in lowerPassword) {
            return "Password cannot contain your username"
        }
        if (emailPrefix.isNotEmpty() && emailPrefix in lowerPassword) {
            return "Password cannot contain your email"
        }

        return null
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}