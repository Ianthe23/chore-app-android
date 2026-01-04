package com.choreapp.android.models

data class Chore(
    val id: Int? = null,
    val title: String,
    val description: String? = null,
    val status: String = "pending",
    val priority: String = "medium",
    val due_date: String? = null,
    val points: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
    val user_id: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_name: String? = null,
    val photo_url: String? = null,
    val photo_path: String? = null
)