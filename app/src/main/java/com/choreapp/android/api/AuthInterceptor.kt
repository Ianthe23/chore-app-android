package com.choreapp.android.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Get token from SharedPreferences
        val sharedPrefs = context.getSharedPreferences("ChoreAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("jwt_token", null)

        // Debug logging
        android.util.Log.d("AuthInterceptor", "Token retrieved: ${if (token.isNullOrEmpty()) "NULL/EMPTY" else "EXISTS (${token.take(20)}...)"}")
        android.util.Log.d("AuthInterceptor", "Request URL: ${chain.request().url}")

        // Add Authorization header if token exists
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            android.util.Log.d("AuthInterceptor", "Authorization header added")
        } else {
            android.util.Log.w("AuthInterceptor", "No token found - request will be unauthorized")
        }

        return chain.proceed(requestBuilder.build())
    }
}