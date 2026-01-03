package com.choreapp.android.api

import com.choreapp.android.models.Chore
import com.choreapp.android.models.ChoreResponse
import com.choreapp.android.models.LoginRequest
import com.choreapp.android.models.LoginResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: LoginRequest): Response<LoginResponse>

    @GET("chores")
    suspend fun getChores(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 5,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null
    ): Response<ChoreResponse>

    @GET("chores/{id}")
    suspend fun getChore(@Path("id") id: Int): Response<ChoreResponse>

    @POST("chores")
    suspend fun createChore(@Body chore: Chore): Response<ChoreResponse>

    @PUT("chores/{id}")
    suspend fun updateChore(@Path("id") id: Int, @Body chore: Chore): Response<ChoreResponse>

    @DELETE("chores/{id}")
    suspend fun deleteChore(@Path("id") id: Int): Response<ChoreResponse>
}