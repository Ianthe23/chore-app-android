package com.choreapp.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.choreapp.android.api.RetrofitClient
import com.choreapp.android.databinding.ActivityRegisterBinding
import com.choreapp.android.models.LoginRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(username, password, confirmPassword)) {
                performRegister(username, password)
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            finish() // Go back to login
        }
    }

    private fun validateInput(username: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun performRegister(username: String, password: String) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@RegisterActivity)
                val registerRequest = LoginRequest(username, password)
                val response = apiService.register(registerRequest)

                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val registerResponse = response.body()!!

                    // Save JWT token to SharedPreferences
                    saveToken(registerResponse.token)
                    saveUserId(registerResponse.user.id)
                    saveUsername(registerResponse.user.username)

                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created successfully! Welcome, ${registerResponse.user.username}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToChoreList()
                } else {
                    // Registration failed
                    val errorMessage = when (response.code()) {
                        400 -> "Username already exists or invalid input"
                        else -> "Registration failed. Please try again."
                    }
                    binding.tvError.text = errorMessage
                    binding.tvError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                binding.tvError.text = "Network error. Please try again."
                binding.tvError.visibility = View.VISIBLE

                e.printStackTrace()
            }
        }
    }

    private fun saveToken(token: String) {
        android.util.Log.d("RegisterActivity", "Saving token: ${token.take(20)}...")
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("jwt_token", token).apply()

        // Verify token was saved
        val savedToken = sharedPrefs.getString("jwt_token", null)
        android.util.Log.d("RegisterActivity", "Token saved successfully: ${!savedToken.isNullOrEmpty()}")
    }

    private fun saveUserId(userId: Int) {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("user_id", userId).apply()
    }

    private fun saveUsername(username: String) {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("username", username).apply()
    }

    private fun navigateToChoreList() {
        val intent = Intent(this, ChoreListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
