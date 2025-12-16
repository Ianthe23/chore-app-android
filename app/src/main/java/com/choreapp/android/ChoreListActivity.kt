package com.choreapp.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.choreapp.android.adapters.ChoreAdapter
import com.choreapp.android.api.RetrofitClient
import com.choreapp.android.databinding.ActivityChoreListBinding
import com.choreapp.android.models.Chore
import kotlinx.coroutines.launch

class ChoreListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChoreListBinding
    private lateinit var choreAdapter: ChoreAdapter
    private var chores: MutableList<Chore> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoreListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupListeners()
        loadChores()
    }

    private fun setupRecyclerView() {
        choreAdapter = ChoreAdapter(chores) { chore ->
            openChoreDetail(chore)
        }

        binding.rvChores.apply {
            layoutManager = LinearLayoutManager(this@ChoreListActivity)
            adapter = choreAdapter
        }
    }

    private fun setupListeners() {
        // Floating action button to add new chore
        binding.fabAddChore.setOnClickListener {
            openChoreDetail(null)
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadChores()
        }
    }

    private fun loadChores() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@ChoreListActivity)
                val response = apiService.getChores()

                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val choreResponse = response.body()!!
                    chores.clear()
                    choreResponse.chores?.let { chores.addAll(it) }
                    choreAdapter.updateChores(chores)

                    if (chores.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this@ChoreListActivity,
                        "Failed to load chores: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false

                Toast.makeText(
                    this@ChoreListActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()
            }
        }
    }

    private fun openChoreDetail(chore: Chore?) {
        val intent = Intent(this, ChoreDetailActivity::class.java)
        chore?.let {
            intent.putExtra("chore_id", it.id)
            intent.putExtra("chore_title", it.title)
            intent.putExtra("chore_description", it.description)
            intent.putExtra("chore_status", it.status)
            intent.putExtra("chore_priority", it.priority)
            intent.putExtra("chore_due_date", it.due_date)
            intent.putExtra("chore_points", it.points)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reload chores when returning from detail activity
        loadChores()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chore_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_refresh -> {
                loadChores()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        // Clear token from SharedPreferences
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()

        // Navigate back to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}