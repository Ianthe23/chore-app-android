package com.choreapp.android.repository

import android.content.Context
import android.util.Log
import com.choreapp.android.api.RetrofitClient
import com.choreapp.android.database.ChoreDatabase
import com.choreapp.android.database.ChoreEntity
import com.choreapp.android.database.SyncQueueEntity
import com.choreapp.android.models.Chore
import com.choreapp.android.network.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChoreRepository(private val context: Context) {
    private val database = ChoreDatabase.getDatabase(context)
    private val choreDao = database.choreDao()
    private val syncQueueDao = database.syncQueueDao()
    private val networkMonitor = NetworkMonitor(context)
    private val gson = Gson()
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "ChoreRepository"
        private var tempIdCounter = -1
    }

    // Flow of all chores from local database
    val chores: Flow<List<Chore>> = choreDao.getAllChores().map { entities ->
        entities.map { it.toChore() }
    }

    // Get pending operations count
    suspend fun getPendingOperationsCount(): Int {
        return syncQueueDao.getPendingOperationsCount()
    }

    // Fetch chores from server and cache locally
    suspend fun fetchChoresFromServer(page: Int = 1, limit: Int = 100) {
        try {
            val apiService = RetrofitClient.getApiService(context)
            val response = apiService.getChores(page, limit)

            if (response.isSuccessful && response.body() != null) {
                val choreList = response.body()!!.items ?: response.body()!!.chores ?: emptyList()

                // Clear old chores and insert fresh data
                if (page == 1) {
                    choreDao.deleteAllChores()
                }

                val entities = choreList.map { ChoreEntity.fromChore(it, isSynced = true) }
                choreDao.insertChores(entities)

                Log.d(TAG, "Fetched ${choreList.size} chores from server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chores from server", e)
            throw e
        }
    }

    // Create chore (works offline)
    suspend fun createChore(chore: Chore) {
        val isOnline = networkMonitor.isCurrentlyOnline()

        if (isOnline) {
            try {
                // Try to create on server
                val apiService = RetrofitClient.getApiService(context)
                val response = apiService.createChore(chore)

                if (response.isSuccessful && response.body() != null) {
                    val createdChore = response.body()!!.chore ?: return

                    // Save to local database
                    choreDao.insertChore(ChoreEntity.fromChore(createdChore, isSynced = true))
                    Log.d(TAG, "Created chore on server: ${createdChore.id}")
                } else {
                    // Server error, queue for later
                    queueCreateOperation(chore)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating chore on server, queueing", e)
                queueCreateOperation(chore)
            }
        } else {
            // Offline: queue operation
            queueCreateOperation(chore)
        }
    }

    private suspend fun queueCreateOperation(chore: Chore) {
        // Generate temporary negative ID
        val tempId = tempIdCounter--

        // Create local entity with temp ID
        val entity = ChoreEntity.fromChore(
            chore.copy(id = tempId),
            isSynced = false
        )
        choreDao.insertChore(entity)

        // Queue sync operation
        val payload = gson.toJson(chore)
        syncQueueDao.insertOperation(
            SyncQueueEntity(
                operation = "CREATE",
                choreId = null,
                tempId = tempId,
                payload = payload
            )
        )

        Log.d(TAG, "Queued CREATE operation for offline sync (tempId: $tempId)")
    }

    // Update chore (works offline)
    suspend fun updateChore(id: Int, chore: Chore) {
        val isOnline = networkMonitor.isCurrentlyOnline()

        if (isOnline) {
            try {
                val apiService = RetrofitClient.getApiService(context)
                val response = apiService.updateChore(id, chore)

                if (response.isSuccessful && response.body() != null) {
                    val updatedChore = response.body()!!.chore ?: return
                    choreDao.insertChore(ChoreEntity.fromChore(updatedChore, isSynced = true))
                    Log.d(TAG, "Updated chore on server: $id")
                } else {
                    queueUpdateOperation(id, chore)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating chore on server, queueing", e)
                queueUpdateOperation(id, chore)
            }
        } else {
            queueUpdateOperation(id, chore)
        }
    }

    private suspend fun queueUpdateOperation(id: Int, chore: Chore) {
        // Update local entity
        val entity = ChoreEntity.fromChore(chore.copy(id = id), isSynced = false)
        choreDao.insertChore(entity)

        // Queue sync operation
        val payload = gson.toJson(chore)
        syncQueueDao.insertOperation(
            SyncQueueEntity(
                operation = "UPDATE",
                choreId = id,
                tempId = null,
                payload = payload
            )
        )

        Log.d(TAG, "Queued UPDATE operation for offline sync (id: $id)")
    }

    // Delete chore (works offline)
    suspend fun deleteChore(id: Int) {
        val isOnline = networkMonitor.isCurrentlyOnline()

        if (isOnline) {
            try {
                val apiService = RetrofitClient.getApiService(context)
                val response = apiService.deleteChore(id)

                if (response.isSuccessful) {
                    choreDao.deleteChoreById(id)
                    Log.d(TAG, "Deleted chore on server: $id")
                } else {
                    queueDeleteOperation(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting chore on server, queueing", e)
                queueDeleteOperation(id)
            }
        } else {
            queueDeleteOperation(id)
        }
    }

    private suspend fun queueDeleteOperation(id: Int) {
        // Mark for deletion locally but keep the entity
        choreDao.updateSyncStatus(id, false)

        // Queue sync operation
        syncQueueDao.insertOperation(
            SyncQueueEntity(
                operation = "DELETE",
                choreId = id,
                tempId = null,
                payload = "{}"
            )
        )

        Log.d(TAG, "Queued DELETE operation for offline sync (id: $id)")
    }

    // Sync all pending operations
    suspend fun syncPendingOperations(): Int = syncMutex.withLock {
        if (!networkMonitor.isCurrentlyOnline()) {
            Log.d(TAG, "Cannot sync: offline")
            return@withLock 0
        }

        val pendingOperations = syncQueueDao.getAllPendingOperations()
        if (pendingOperations.isEmpty()) {
            Log.d(TAG, "No pending operations to sync")
            return@withLock 0
        }

        Log.d(TAG, "Syncing ${pendingOperations.size} pending operations")
        var syncedCount = 0
        val apiService = RetrofitClient.getApiService(context)

        for (operation in pendingOperations) {
            try {
                when (operation.operation) {
                    "CREATE" -> {
                        val chore = gson.fromJson(operation.payload, Chore::class.java)
                        Log.d(TAG, "Attempting to sync CREATE: ${chore.title}")
                        val response = apiService.createChore(chore)

                        Log.d(TAG, "CREATE response: code=${response.code()}, successful=${response.isSuccessful}")

                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null && body.chore != null) {
                                val createdChore = body.chore

                                // Delete sync queue entry FIRST to prevent duplicates
                                syncQueueDao.deleteOperation(operation)

                                // Replace temp ID with real ID
                                operation.tempId?.let { tempId ->
                                    choreDao.deleteChoreById(tempId)
                                }
                                choreDao.insertChore(ChoreEntity.fromChore(createdChore, isSynced = true))

                                syncedCount++
                                Log.d(TAG, "✓ Synced CREATE operation: ${createdChore.id} - ${createdChore.title}")
                            } else {
                                Log.e(TAG, "✗ CREATE response body or chore is null")
                                // Delete operation anyway to prevent infinite retry
                                syncQueueDao.deleteOperation(operation)
                            }
                        } else {
                            Log.e(TAG, "✗ CREATE failed with code: ${response.code()}, message: ${response.message()}")
                            // For client errors (4xx), delete the operation to prevent infinite retry
                            if (response.code() in 400..499) {
                                Log.w(TAG, "Client error - removing operation from queue")
                                syncQueueDao.deleteOperation(operation)
                            }
                        }
                    }
                    "UPDATE" -> {
                        val chore = gson.fromJson(operation.payload, Chore::class.java)
                        val response = apiService.updateChore(operation.choreId!!, chore)

                        if (response.isSuccessful && response.body() != null) {
                            val updatedChore = response.body()!!.chore!!
                            choreDao.insertChore(ChoreEntity.fromChore(updatedChore, isSynced = true))

                            syncQueueDao.deleteOperation(operation)
                            syncedCount++
                            Log.d(TAG, "Synced UPDATE operation: ${operation.choreId}")
                        }
                    }
                    "DELETE" -> {
                        val response = apiService.deleteChore(operation.choreId!!)

                        if (response.isSuccessful) {
                            choreDao.deleteChoreById(operation.choreId)
                            syncQueueDao.deleteOperation(operation)
                            syncedCount++
                            Log.d(TAG, "Synced DELETE operation: ${operation.choreId}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing operation: ${operation.operation}", e)
                // Keep operation in queue for retry
            }
        }

        Log.d(TAG, "Sync completed: $syncedCount/${pendingOperations.size} operations synced")
        return@withLock syncedCount
    }
}