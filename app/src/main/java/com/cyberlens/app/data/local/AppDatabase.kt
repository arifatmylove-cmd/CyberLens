package com.cyberlens.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cyberlens.app.domain.model.ScanType

class Converters {
    @TypeConverter
    fun fromScanType(value: ScanType): String = value.name

    @TypeConverter
    fun toScanType(value: String): ScanType = ScanType.valueOf(value)
}

@Database(entities = [ScanEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        const val DATABASE_NAME = "cyberlens_db"
    }
}
