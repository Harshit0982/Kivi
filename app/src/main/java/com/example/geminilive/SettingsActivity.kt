package com.example.geminilive

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geminilive.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var keyManager: KeyManager
    private lateinit var adapter: KeysAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keyManager = KeyManager.getInstance(this)

        binding.backButton.setOnClickListener { finish() }

        adapter = KeysAdapter(
            keys = keyManager.getAllKeys(),
            onDelete = { key -> confirmDelete(key) },
            onEdit = { key -> showEditDialog(key) }
        )

        binding.keysRecycler.layoutManager = LinearLayoutManager(this)
        binding.keysRecycler.adapter = adapter

        binding.addKeyButton.setOnClickListener { showAddDialog() }
        binding.statsText.text = keyManager.getStats()

        if (keyManager.getAllKeys().isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.keysRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.keysRecycler.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.update(keyManager.getAllKeys())
        binding.statsText.text = keyManager.getStats()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_key, null)
        val keyInput = dialogView.findViewById<EditText>(R.id.keyInput)
        val labelInput = dialogView.findViewById<EditText>(R.id.labelInput)

        AlertDialog.Builder(this)
            .setTitle("Add API Key")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val key = keyInput.text.toString().trim()
                val label = labelInput.text.toString().trim()
                if (key.isNotEmpty()) {
                    keyManager.addKey(key, label)
                    adapter.update(keyManager.getAllKeys())
                    binding.statsText.text = keyManager.getStats()
                    binding.emptyState.visibility = View.GONE
                    binding.keysRecycler.visibility = View.VISIBLE
                    Toast.makeText(this, "Key added ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Key cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(key: ApiKey) {
        val input = EditText(this).apply {
            setText(key.label)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle("Rename Key")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newLabel = input.text.toString().trim()
                keyManager.updateLabel(key.id, newLabel)
                adapter.update(keyManager.getAllKeys())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(key: ApiKey) {
        AlertDialog.Builder(this)
            .setTitle("Delete Key?")
            .setMessage("Remove '${key.label}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                keyManager.removeKey(key.id)
                adapter.update(keyManager.getAllKeys())
                binding.statsText.text = keyManager.getStats()
                if (keyManager.getAllKeys().isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.keysRecycler.visibility = View.GONE
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
