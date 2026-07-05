package com.cyberlens.app.data.local

import androidx.room.*
import com.cyberlens.app.domain.model.ScanType
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE scanType = :type ORDER BY timestamp DESC")
    fun getScansByType(type: ScanType): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE target LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScans(query: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getScanById(id: Long): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()

    @Query("SELECT COUNT(*) FROM scans")
    suspend fun getScanCount(): Int
}
