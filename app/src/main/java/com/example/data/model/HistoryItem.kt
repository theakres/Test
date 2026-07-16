package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // "CALCULATOR" or "CONVERTER"
    val description: String,   // E.g., "5 + 5" or "5 km to m"
    val result: String,        // E.g., "10" or "5000"
    val timestamp: Long = System.currentTimeMillis()
)
