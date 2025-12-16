package com.choreapp.android.models

data class ChoreResponse(
    val chores: List<Chore>? = null,
    val chore: Chore? = null,
    val message: String? = null
)