package com.craysafe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.craysafe.auth.LoginResult
import com.craysafe.auth.LoginViewModel
import com.craysafe.databinding.ActivityLoginBinding
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // If already logged in, skip login
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        // Observe login result
        viewModel.loginResult.observe(this) { result ->
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
                    Toast.makeText(this, "✅ Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()

                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fcmToken = task.result

                                lifecycleScope.launch {
                                    try {
                                        com.craysafe.api.ApiClient.apiService.saveFcmToken(
                                            "Bearer ${sessionManager.getToken()}",
                                            fcmToken
                                        )
                                    } catch (e: Exception) {

                                    }
                                }
                            }
                        }
                }
                is LoginResult.Error -> {
                    Toast.makeText(this, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Login button click
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the ViewModel to perform login
            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}