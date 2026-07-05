package com.cyberlens.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyberlens.app.domain.model.ScanType

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanType: ScanType,
    val target: String,
    val resultJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val riskLevel: String = "UNKNOWN",
    val summary: String = ""
)
