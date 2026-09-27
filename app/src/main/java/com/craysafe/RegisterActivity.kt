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

            // Client-side validation with inline errors
            if (username.isEmpty()) {
                binding.etUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (username.length < 3) {
                binding.etUsername.error = "At least 3 characters"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter a valid email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.etPassword.error = "At least 6 characters"
                return@setOnClickListener
            }
            if (password != confirm) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (productId.isEmpty()) {
                binding.etProductId.error = "Product ID is required"
                return@setOnClickListener
            }

            viewModel.register(username, email, password, confirm, productId)
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}