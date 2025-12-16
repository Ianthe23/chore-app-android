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
    val user_id: Int? = null
)