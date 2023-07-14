package com.abdulkadirkara.paint

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Paint::class], version = 1)
abstract class PaintDB: RoomDatabase() {
    abstract fun paintDao():PaintDao
}