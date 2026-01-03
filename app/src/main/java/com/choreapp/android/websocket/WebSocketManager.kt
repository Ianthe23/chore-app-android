package com.choreapp.android.websocket

import android.util.Log
import com.choreapp.android.models.Chore
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketManager private constructor() {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var listeners = mutableListOf<WebSocketListener>()
    private var isConnected = false
    private var userId: Int? = null
    private val gson = Gson()

    companion object {
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }

        private const val TAG = "WebSocketManager"
    }

    interface WebSocketListener {
        fun onChoreCreated(chore: Chore)
        fun onChoreUpdated(chore: Chore)
        fun onChoreDeleted(choreId: Int)
        fun onConnected()
        fun onDisconnected()
    }

    fun connect(userId: Int) {
        if (isConnected && this.userId == userId) {
            Log.d(TAG, "Already connected for user $userId")
            return
        }

        disconnect()
        this.userId = userId

        val request = Request.Builder()
            .url("ws://10.0.2.2:3000")
            .build()

        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true

                // Send authentication message
                val authMessage = JsonObject().apply {
                    addProperty("type", "AUTH")
                    addProperty("userId", userId)
                }
                webSocket.send(authMessage.toString())
                Log.d(TAG, "Sent AUTH message for user $userId")

                notifyConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val type = json.get("type")?.asString

                    when (type) {
                        "CHORE_CREATED" -> {
                            val chore = gson.fromJson(json.get("chore"), Chore::class.java)
                            notifyChoreCreated(chore)
                        }
                        "CHORE_UPDATED" -> {
                            val chore = gson.fromJson(json.get("chore"), Chore::class.java)
                            notifyChoreUpdated(chore)
                        }
                        "CHORE_DELETED" -> {
                            val choreId = json.get("choreId")?.asInt ?: return
                            notifyChoreDeleted(choreId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                isConnected = false
                notifyDisconnected()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                isConnected = false
                notifyDisconnected()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        isConnected = false
        userId = null
    }

    fun addListener(listener: WebSocketListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    private fun notifyChoreCreated(chore: Chore) {
        listeners.forEach { it.onChoreCreated(chore) }
    }

    private fun notifyChoreUpdated(chore: Chore) {
        listeners.forEach { it.onChoreUpdated(chore) }
    }

    private fun notifyChoreDeleted(choreId: Int) {
        listeners.forEach { it.onChoreDeleted(choreId) }
    }

    private fun notifyConnected() {
        listeners.forEach { it.onConnected() }
    }

    private fun notifyDisconnected() {
        listeners.forEach { it.onDisconnected() }
    }

    fun isConnected(): Boolean = isConnected
}