package com.choreapp.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.choreapp.android.adapters.ChoreAdapter
import com.choreapp.android.databinding.ActivityChoreListBinding
import com.choreapp.android.models.Chore
import com.choreapp.android.network.NetworkMonitor
import com.choreapp.android.repository.ChoreRepository
import com.choreapp.android.utils.NotificationHelper
import com.choreapp.android.utils.ShakeDetector
import com.choreapp.android.websocket.WebSocketManager
import com.choreapp.android.workers.SyncWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ChoreListActivity : AppCompatActivity(), WebSocketManager.WebSocketListener {

    private lateinit var binding: ActivityChoreListBinding
    private lateinit var choreAdapter: ChoreAdapter
    private var chores: MutableList<Chore> = mutableListOf()
    private var allChores: MutableList<Chore> = mutableListOf()

    // Offline-first architecture
    private lateinit var repository: ChoreRepository
    private lateinit var networkMonitor: NetworkMonitor
    private val webSocketManager = WebSocketManager.getInstance()

    // Shake detector for refresh (3p requirement - sensors)
    private lateinit var shakeDetector: ShakeDetector

    // Pagination
    private var currentPage = 1
    private var pageSize = 10
    private var totalPages = 1

    private var isOnline = true
    private var pendingOperationsCount = 0

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoreListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Initialize offline-first components
        repository = ChoreRepository(this)
        networkMonitor = NetworkMonitor(this)

        // Initialize shake detector (3p requirement - sensors)
        shakeDetector = ShakeDetector(this) {
            onShakeDetected()
        }

        // Request notification permission
        requestNotificationPermission()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        setupRecyclerView()
        setupListeners()
        setupWebSocket()
        setupNetworkMonitoring()
        setupBackgroundSync()
        loadChoresFromDatabase()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun setupNetworkMonitoring() {
        // Monitor network status
        lifecycleScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                isOnline = online
                runOnUiThread {
                    updateNetworkStatus()

                    if (online) {
                        // Trigger sync when coming back online
                        lifecycleScope.launch {
                            syncPendingOperations()
                            // Update UI to reflect sync completion
                            pendingOperationsCount = repository.getPendingOperationsCount()
                            updateNetworkStatus()
                            // Small delay before fetching to ensure server has processed
                            kotlinx.coroutines.delay(500)
                            fetchFromServer()
                        }
                    }
                }
            }
        }

        // Update pending operations count periodically
        lifecycleScope.launch {
            while (true) {
                pendingOperationsCount = repository.getPendingOperationsCount()
                runOnUiThread {
                    updateNetworkStatus()
                }
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
            }
        }
    }

    private fun updateNetworkStatus() {
        binding.tvNetworkStatus.visibility = View.VISIBLE
        binding.tvNetworkStatus.text = if (isOnline) {
            if (pendingOperationsCount > 0) {
                "Online - Syncing $pendingOperationsCount pending operation(s)..."
            } else {
                "Online"
            }
        } else {
            "Offline - Changes will sync when back online${if (pendingOperationsCount > 0) " ($pendingOperationsCount pending)" else ""}"
        }

        binding.tvNetworkStatus.setBackgroundColor(
            if (isOnline) {
                if (pendingOperationsCount > 0) 0xFFFFA500.toInt() // Orange
                else 0xFF4CAF50.toInt() // Green
            } else {
                0xFF9E9E9E.toInt() // Gray
            }
        )
    }

    private fun setupBackgroundSync() {
        // Setup periodic background sync with WorkManager (3p requirement)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "chore_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
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
        binding.fabAddChore.setOnClickListener {
            openChoreDetail(null)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                if (isOnline) {
                    fetchFromServer()
                } else {
                    Toast.makeText(
                        this@ChoreListActivity,
                        "Cannot refresh while offline",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        // Pagination buttons
        binding.btnPrevious.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePagedChores()
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePagedChores()
            }
        }
    }

    private fun loadChoresFromDatabase() {
        lifecycleScope.launch {
            repository.chores.collectLatest { choreList ->
                allChores.clear()
                allChores.addAll(choreList)

                // Reset to first page when data changes
                currentPage = 1
                updatePagedChores()

                binding.tvEmptyState.visibility = if (allChores.isEmpty()) View.VISIBLE else View.GONE
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updatePagedChores() {
        // Calculate total pages
        totalPages = if (allChores.isEmpty()) 1 else ((allChores.size - 1) / pageSize) + 1

        // Get chores for current page
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, allChores.size)

        chores.clear()
        if (startIndex < allChores.size) {
            chores.addAll(allChores.subList(startIndex, endIndex))
        }

        choreAdapter.updateChores(chores)
        updatePaginationUI()

        // Scroll to top of list when changing pages
        binding.rvChores.scrollToPosition(0)
    }

    private fun updatePaginationUI() {
        binding.tvPageInfo.text = "Page $currentPage of $totalPages"
        binding.btnPrevious.isEnabled = currentPage > 1
        binding.btnNext.isEnabled = currentPage < totalPages

        // Show/hide pagination layout based on whether we have multiple pages
        binding.paginationLayout.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
    }

    private suspend fun fetchFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        try {
            repository.fetchChoresFromServer()
            binding.swipeRefreshLayout.isRefreshing = false
        } catch (e: Exception) {
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(
                this,
                "Failed to fetch from server: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.progressBar.visibility = View.GONE
    }

    private suspend fun syncPendingOperations() {
        if (!isOnline) return

        try {
            val syncedCount = repository.syncPendingOperations()
            if (syncedCount > 0) {
                Toast.makeText(
                    this,
                    "Synced $syncedCount operation(s)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ChoreListActivity", "Sync error", e)
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

            // Pass location data (3p requirement - Location & Maps)
            it.latitude?.let { lat -> intent.putExtra("chore_latitude", lat) }
            it.longitude?.let { lon -> intent.putExtra("chore_longitude", lon) }
            it.location_name?.let { name -> intent.putExtra("chore_location_name", name) }
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Start shake detector (3p requirement - sensors)
        shakeDetector.start()

        // Only fetch from server, don't sync (sync is triggered by network status change)
        lifecycleScope.launch {
            if (isOnline) {
                // Update pending count in case it changed
                pendingOperationsCount = repository.getPendingOperationsCount()
                updateNetworkStatus()

                // Only fetch if there are no pending operations
                if (pendingOperationsCount == 0) {
                    fetchFromServer()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop shake detector to save battery
        shakeDetector.stop()
    }

    private fun onShakeDetected() {
        runOnUiThread {
            Toast.makeText(this, "Shake detected - Refreshing...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                if (isOnline) {
                    syncPendingOperations()
                    pendingOperationsCount = repository.getPendingOperationsCount()
                    updateNetworkStatus()
                    kotlinx.coroutines.delay(500)
                    fetchFromServer()
                } else {
                    Toast.makeText(
                        this@ChoreListActivity,
                        "Cannot refresh while offline",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                lifecycleScope.launch {
                    if (isOnline) {
                        syncPendingOperations()
                        // Update UI to reflect sync completion
                        pendingOperationsCount = repository.getPendingOperationsCount()
                        updateNetworkStatus()
                        // Small delay before fetching
                        kotlinx.coroutines.delay(500)
                        fetchFromServer()
                    } else {
                        Toast.makeText(
                            this@ChoreListActivity,
                            "Cannot refresh while offline",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        webSocketManager.removeListener(this)
        webSocketManager.disconnect()

        val sharedPrefs = getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.removeListener(this)
    }

    // WebSocket listener methods (with notifications - 2p requirement)
    override fun onChoreCreated(chore: Chore) {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Chore created - ${chore.title}")
            NotificationHelper.showChoreCreatedNotification(this, chore)

            // Refresh from server to get the latest data
            lifecycleScope.launch {
                fetchFromServer()
            }
        }
    }

    override fun onChoreUpdated(chore: Chore) {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Chore updated - ${chore.title}")
            NotificationHelper.showChoreUpdatedNotification(this, chore)

            lifecycleScope.launch {
                fetchFromServer()
            }
        }
    }

    override fun onChoreDeleted(choreId: Int) {
        runOnUiThread {
            android.util.Log.d("ChoreListActivity", "WebSocket: Chore deleted - ID $choreId")

            val deletedChore = chores.find { it.id == choreId }
            if (deletedChore != null) {
                NotificationHelper.showChoreDeletedNotification(this, choreId, deletedChore.title)
            }

            lifecycleScope.launch {
                fetchFromServer()
            }
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