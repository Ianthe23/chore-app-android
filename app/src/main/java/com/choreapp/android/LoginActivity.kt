package com.choreapp.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.choreapp.android.api.RetrofitClient
import com.choreapp.android.databinding.ActivityLoginBinding
import com.choreapp.android.models.LoginRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToChoreList()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(username, password)) {
                performLogin(username, password)
            }
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            return false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        return true
    }

    private fun performLogin(username: String, password: String) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@LoginActivity)
                val loginRequest = LoginRequest(username, password)
                val response = apiService.login(loginRequest)

                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Save JWT token to SharedPreferences
                    saveToken(loginResponse.token)
                    saveUserId(loginResponse.user.id)
                    saveUsername(loginResponse.user.username)

                    Toast.makeText(
                        this@LoginActivity,
                        "Welcome, ${loginResponse.user.username}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToChoreList()
                } else {
                    // Login failed
                    binding.tvError.text = getString(R.string.login_error)
                    binding.tvError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                binding.tvError.text = getString(R.string.network_error)
                binding.tvError.visibility = View.VISIBLE

                e.printStackTrace()
            }
        }
    }

    private fun saveToken(token: String) {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("jwt_token", token).apply()
    }

    private fun saveUserId(userId: Int) {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("user_id", userId).apply()
    }

    private fun saveUsername(username: String) {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("username", username).apply()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("jwt_token", null)
        return !token.isNullOrEmpty()
    }

    private fun navigateToChoreList() {
        val intent = Intent(this, ChoreListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
