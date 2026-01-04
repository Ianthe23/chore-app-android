package com.choreapp.android.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.choreapp.android.models.Chore

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val due_date: String?,
    val points: Int,
    val created_at: String?,
    val updated_at: String?,
    val user_id: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val location_name: String?,
    val photo_url: String?,
    val photo_path: String?,
    val isSynced: Boolean = true // Track if synced with server
) {
    fun toChore(): Chore {
        return Chore(
            id = id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            due_date = due_date,
            points = points,
            created_at = created_at,
            updated_at = updated_at,
            user_id = user_id,
            latitude = latitude,
            longitude = longitude,
            location_name = location_name,
            photo_url = photo_url,
            photo_path = photo_path
        )
    }

    companion object {
        fun fromChore(chore: Chore, isSynced: Boolean = true): ChoreEntity {
            return ChoreEntity(
                id = chore.id ?: 0,
                title = chore.title,
                description = chore.description,
                status = chore.status,
                priority = chore.priority,
                due_date = chore.due_date,
                points = chore.points,
                created_at = chore.created_at,
                updated_at = chore.updated_at,
                user_id = chore.user_id,
                latitude = chore.latitude,
                longitude = chore.longitude,
                location_name = chore.location_name,
                photo_url = chore.photo_url,
                photo_path = chore.photo_path,
                isSynced = isSynced
            )
        }
    }
}