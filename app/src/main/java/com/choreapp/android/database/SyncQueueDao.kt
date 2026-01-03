package com.choreapp.android.database

import androidx.room.*

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllPendingOperations(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingOperationsCount(): Int

    @Insert
    suspend fun insertOperation(operation: SyncQueueEntity): Long

    @Delete
    suspend fun deleteOperation(operation: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteOperationById(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllOperations()
}