package com.choreapp.android.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val choreId: Int?, // Null for CREATE (temp ID), non-null for UPDATE/DELETE
    val tempId: Int?, // Temporary negative ID for CREATE operations
    val payload: String, // JSON payload for CREATE/UPDATE
    val timestamp: Long = System.currentTimeMillis()
)