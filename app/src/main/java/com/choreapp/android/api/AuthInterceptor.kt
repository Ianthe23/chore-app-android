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

        // Add Authorization header if token exists
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}