package com.example.geminilive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geminilive.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var liveClient: GeminiLiveClient? = null
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var keyManager: KeyManager
    private var isConnected = false
    private var isMicOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keyManager = KeyManager.getInstance(this)
        audioPlayer = AudioPlayer()

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.micButton.setOnClickListener {
            if (!isConnected) {
                connect()
            } else {
                toggleMic()
            }
        }

        updateKeyInfo()
    }

    override fun onResume() {
        super.onResume()
        updateKeyInfo()
    }

    private fun updateKeyInfo() {
        val keys = keyManager.getAllKeys()
        if (keys.isEmpty()) {
            binding.statusText.text = "Tap ⚙️ to add API keys"
            binding.keyInfoText.text = ""
        } else {
            binding.statusText.text = "Tap 🎙 to start conversation"
            binding.keyInfoText.text = keyManager.getStats()
        }
    }

    private fun connect() {
        if (!keyManager.hasKeys()) {
            Toast.makeText(this, "Please add an API key in settings first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
            return
        }

        val apiKey = keyManager.getNextAvailableKey()
        if (apiKey == null) {
            Toast.makeText(this, "All keys are rate-limited. Try later.", Toast.LENGTH_LONG).show()
            return
        }

        keyManager.markUsed(apiKey.id)
        binding.keyInfoText.text = "Using: ${apiKey.label} | ${keyManager.getStats()}"

        liveClient = GeminiLiveClient(
            apiKey = apiKey.value,
            keyId = apiKey.id,
            keyManager = keyManager,
            onStatusUpdate = { status ->
                runOnUiThread { binding.statusText.text = status }
            },
            onKeyRotated = { newLabel ->
                runOnUiThread { binding.keyInfoText.text = "🔄 Rotated to: $newLabel" }
            },
            onTranscriptUpdate = { text ->
                runOnUiThread {
                    binding.transcriptText.append("\n$text")
                    binding.transcriptScroll.post {
                        binding.transcriptScroll.fullScroll(android.view.View.FOCUS_DOWN)
                    }
                }
            },
            onAudioReceived = { audioData ->
                audioPlayer.play(audioData)
            }
        )

        liveClient?.connect()
        isConnected = true
        Toast.makeText(this, "Connected with ${apiKey.label}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleMic() {
        isMicOn = !isMicOn
        if (isMicOn) {
            liveClient?.startMic()
            binding.statusText.text = "🎤 Listening..."
        } else {
            liveClient?.stopMic()
            binding.statusText.text = "Tap mic to talk"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        liveClient?.disconnect()
        audioPlayer.release()
    }
}
