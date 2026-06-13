package com.example.data.api

import android.util.Log
import com.example.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketManager(private val okHttpClient: OkHttpClient) {

    private var webSocket: WebSocket? = null
    
    // Emissions pour UI
    private val _messageEvents = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val messageEvents: SharedFlow<JSONObject> = _messageEvents

    fun connect(userId: String) {
        val request = Request.Builder()
            .url(Constants.WS_URL + userId)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    _messageEvents.tryEmit(json)
                } catch (e: Exception) {
                    Log.e("WS", "Error parsing message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS", "Closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "Failure", t)
                // Implement reconnect logic here if needed
            }
        })
    }

    fun sendMessage(receiverId: String, content: String, senderUsername: String): Boolean {
        val ws = webSocket
        if (ws != null) {
            val json = JSONObject().apply {
                put("type", "message")
                put("receiver_id", receiverId)
                put("content", content)
                put("msg_type", "text")
                put("sender_username", senderUsername)
            }
            return ws.send(json.toString())
        }
        return false
    }

    fun joinLive(liveId: String) {
        webSocket?.send(JSONObject().apply {
            put("type", "join_live")
            put("live_id", liveId)
        }.toString())
    }

    fun leaveLive(liveId: String) {
        webSocket?.send(JSONObject().apply {
            put("type", "leave_live")
            put("live_id", liveId)
        }.toString())
    }

    fun sendLiveComment(liveId: String, comment: String) {
        webSocket?.send(JSONObject().apply {
            put("type", "live_comment")
            put("live_id", liveId)
            put("comment", comment)
        }.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User logged out")
        webSocket = null
    }
}
