package com.choreapp.android.models

data class LoginResponse(
    val message: String,
    val user: User,
    val token: String
)