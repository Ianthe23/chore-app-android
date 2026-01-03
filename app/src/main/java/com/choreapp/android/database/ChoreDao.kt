package com.choreapp.android.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {
    @Query("SELECT * FROM chores ORDER BY created_at DESC")
    fun getAllChores(): Flow<List<ChoreEntity>>

    @Query("SELECT * FROM chores WHERE id = :id")
    suspend fun getChoreById(id: Int): ChoreEntity?

    @Query("SELECT * FROM chores WHERE isSynced = 0")
    suspend fun getUnsyncedChores(): List<ChoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChore(chore: ChoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChores(chores: List<ChoreEntity>)

    @Update
    suspend fun updateChore(chore: ChoreEntity)

    @Delete
    suspend fun deleteChore(chore: ChoreEntity)

    @Query("DELETE FROM chores WHERE id = :id")
    suspend fun deleteChoreById(id: Int)

    @Query("DELETE FROM chores")
    suspend fun deleteAllChores()

    @Query("UPDATE chores SET isSynced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, synced: Boolean)
}