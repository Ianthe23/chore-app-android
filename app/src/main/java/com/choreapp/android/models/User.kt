package com.choreapp.android.models

data class User(
    val id: Int,
    val username: String,
    val email: String? = null
)