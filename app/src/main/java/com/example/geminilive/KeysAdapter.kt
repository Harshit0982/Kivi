package com.example.geminilive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.geminilive.databinding.ItemKeyBinding
import java.text.SimpleDateFormat
import java.util.*

class KeysAdapter(
    private var keys: List<ApiKey>,
    private val onDelete: (ApiKey) -> Unit,
    private val onEdit: (ApiKey) -> Unit
) : RecyclerView.Adapter<KeysAdapter.KeyViewHolder>() {

    inner class KeyViewHolder(val binding: ItemKeyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyViewHolder {
        val binding = ItemKeyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KeyViewHolder(binding)
    }

    override fun getItemCount() = keys.size

    override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
        val key = keys[position]
        val now = System.currentTimeMillis()
        val isLimited = key.rateLimitedUntil > now

        holder.binding.apply {
            labelText.text = key.label
            maskedKey.text = maskKey(key.value)
            usageText.text = "Used: ${key.usageCount} times"

            statusText.text = if (isLimited) {
                val mins = (key.rateLimitedUntil - now) / 60_000
                "⏳ Rate-limited (${mins}m)"
            } else {
                "✅ Active"
            }
            statusText.setTextColor(
                if (isLimited) 0xFFFF6B6B.toInt() else 0xFF6BCB77.toInt()
            )

            deleteButton.setOnClickListener { onDelete(key) }
            editButton.setOnClickListener { onEdit(key) }
            root.setOnClickListener { onEdit(key) }
        }
    }

    fun update(newKeys: List<ApiKey>) {
        keys = newKeys
        notifyDataSetChanged()
    }

    private fun maskKey(key: String): String {
        if (key.length <= 8) return "***"
        return key.take(4) + "•••" + key.takeLast(4)
    }
}
