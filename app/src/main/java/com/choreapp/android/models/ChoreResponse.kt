package com.choreapp.android.models

data class ChoreResponse(
    val items: List<Chore>? = null,  // Backend returns "items" not "chores"
    val chores: List<Chore>? = null, // Keep for backward compatibility
    val chore: Chore? = null,
    val message: String? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null
)