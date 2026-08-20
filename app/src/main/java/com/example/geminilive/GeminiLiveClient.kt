package com.example.geminilive

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

class GeminiLiveClient(
    private var apiKey: String,
    private var keyId: String,
    private val keyManager: KeyManager,
    private val onStatusUpdate: (String) -> Unit,
    private val onKeyRotated: (String) -> Unit,
    private val onTranscriptUpdate: (String) -> Unit,
    private val onAudioReceived: (ByteArray) -> Unit
) {
    private val TAG = "GeminiLive"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private var isClosed = false

    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private val LIVE_URL
        get() = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"

    fun connect() {
        if (isClosed) return
        val request = Request.Builder().url(LIVE_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                onStatusUpdate("Connected ✅")
                sendSetupMessage()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                handleServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS closed: $code $reason")
                if (code == 1008 || code == 1013) {
                    handleRateLimit()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS error: ${t.message} code=${response?.code}")
                when (response?.code) {
                    429, 403 -> handleRateLimit()
                    401 -> onStatusUpdate("❌ Invalid API key")
                    else -> onStatusUpdate("Error: ${t.message}")
                }
            }
        })
    }

    /** Auto-rotate to next available key */
    private fun handleRateLimit() {
        keyManager.markRateLimited(keyId)
        val nextKey = keyManager.getNextAvailableKey()
        if (nextKey != null && nextKey.id != keyId) {
            Log.d(TAG, "Rotating key: $keyId -> ${nextKey.id}")
            keyId = nextKey.id
            apiKey = nextKey.value
            keyManager.markUsed(keyId)
            onKeyRotated(nextKey.label)
            onStatusUpdate("🔄 Rotated to ${nextKey.label}")
            // Reconnect with new key
            webSocket?.close(1000, "Rotating")
            scope.launch { delay(300); connect() }
        } else {
            onStatusUpdate("❌ All keys rate-limited")
        }
    }

    private fun sendSetupMessage() {
        val setup = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.5-flash-preview-native-audio-dialog")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Aoede")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "You are a friendly voice assistant. Respond naturally.")
                    }))
                })
            })
        }
        webSocket?.send(setup.toString())
        onStatusUpdate("Ready — Tap mic to talk")
    }

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            // Detect rate limit in payload too
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                if (error.optInt("code") == 429 || error.optString("status") == "RESOURCE_EXHAUSTED") {
                    handleRateLimit()
                    return
                }
            }
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.getJSONArray("parts")
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val audioB64 = part.getJSONObject("inlineData").getString("data")
                            val audioBytes = Base64.decode(audioB64, Base64.DEFAULT)
                            onAudioReceived(audioBytes)
                        }
                    }
                }
                if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                    onTranscriptUpdate("🤖 [Gemini responded]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }

    fun startMic() {
        if (isRecording) return
        isRecording = true
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE * 2
        )
        audioRecord?.startRecording()
        recordingJob = scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    sendAudioChunk(buffer.copyOf(read))
                }
            }
        }
    }

    fun stopMic() {
        isRecording = false
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.send(JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turnComplete", true)
            })
        }.toString())
    }

    private fun sendAudioChunk(pcm: ByteArray) {
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm")
                    put("data", Base64.encodeToString(pcm, Base64.NO_WRAP))
                }))
            })
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        isClosed = true
        isRecording = false
        recordingJob?.cancel()
        audioRecord?.release()
        webSocket?.close(1000, "Bye")
        client.dispatcher.executorService.shutdown()
        scope.cancel()
    }
}
