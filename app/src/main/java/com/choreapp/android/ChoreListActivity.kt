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
import com.choreapp.android.websocket.WebSocketManager
import kotlinx.coroutines.launch

class ChoreListActivity : AppCompatActivity(), WebSocketManager.WebSocketListener {

    private lateinit var binding: ActivityChoreListBinding
    private lateinit var choreAdapter: ChoreAdapter
    private var chores: MutableList<Chore> = mutableListOf()

    // Pagination variables
    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = false
    private val pageSize = 5  // Show 5 chores per page

    // WebSocket
    private val webSocketManager = WebSocketManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoreListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupListeners()
        setupWebSocket()
        loadChores()
    }

    private fun setupWebSocket() {
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("user_id", -1)

        if (userId != -1) {
            webSocketManager.addListener(this)
            webSocketManager.connect(userId)
            android.util.Log.d("ChoreListActivity", "WebSocket setup for user $userId")
        }
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

        // Swipe to refresh - reset to first page
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentPage = 1
            loadChores(reset = true)
        }

        // Previous button
        binding.btnPrevious.setOnClickListener {
            if (currentPage > 1 && !isLoading) {
                currentPage--
                loadChores(reset = true)
            }
        }

        // Next button
        binding.btnNext.setOnClickListener {
            if (currentPage < totalPages && !isLoading) {
                currentPage++
                loadChores(reset = true)
            }
        }
    }

    private fun loadChores(reset: Boolean = false) {
        if (reset) {
            currentPage = 1
        }

        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@ChoreListActivity)
                val response = apiService.getChores(page = currentPage, limit = pageSize)

                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
                isLoading = false

                if (response.isSuccessful && response.body() != null) {
                    val choreResponse = response.body()!!

                    // Calculate total pages
                    val total = choreResponse.total ?: 0
                    totalPages = if (total > 0) ((total + pageSize - 1) / pageSize) else 1

                    // Backend returns "items" not "chores"
                    val choreList = choreResponse.items ?: choreResponse.chores ?: emptyList()

                    // Always replace chores for pagination (not append)
                    chores.clear()
                    chores.addAll(choreList)
                    choreAdapter.updateChores(chores)

                    // Update pagination UI
                    updatePaginationUI()

                    android.util.Log.d("ChoreListActivity", "Loaded ${choreList.size} chores (page $currentPage/$totalPages, total: $total)")

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
                isLoading = false

                Toast.makeText(
                    this@ChoreListActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()
            }
        }
    }

    private fun updatePaginationUI() {
        // Update page info text
        binding.tvPageInfo.text = "Page $currentPage of $totalPages"

        // Enable/disable previous button
        binding.btnPrevious.isEnabled = currentPage > 1

        // Enable/disable next button
        binding.btnNext.isEnabled = currentPage < totalPages

        // Hide pagination if only one page or no chores
        binding.paginationLayout.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
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
        // Reload chores when returning from detail activity - reset to refresh
        currentPage = 1
        loadChores(reset = true)
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
                currentPage = 1
                loadChores(reset = true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        // Disconnect WebSocket
        webSocketManager.removeListener(this)
        webSocketManager.disconnect()

        // Clear token from SharedPreferences
        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()

        // Navigate back to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.removeListener(this)
    }

    // WebSocket listener methods
    override fun onChoreCreated(chore: Chore) {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Chore created - ${chore.title}")
            // Reload to get fresh data and update pagination
            loadChores(reset = true)
        }
    }

    override fun onChoreUpdated(chore: Chore) {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Chore updated - ${chore.title}")
            // Find and update the chore in the list
            val index = chores.indexOfFirst { it.id == chore.id }
            if (index != -1) {
                chores[index] = chore
                choreAdapter.updateChores(chores)
            } else {
                // Chore might be on a different page or filtered out, reload to be safe
                loadChores(reset = true)
            }
        }
    }

    override fun onChoreDeleted(choreId: Int) {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Chore deleted - ID $choreId")
            // Reload to get fresh data and update pagination
            loadChores(reset = true)
        }
    }

    override fun onConnected() {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Connected")
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Disconnected")
        }
    }
}