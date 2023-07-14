package com.abdulkadirkara.paint

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PaintDao {
    @Query("SELECT * FROM paints")
    fun getAll(): List<Paint>

    @Query("SELECT * FROM paints WHERE imageName LIKE :filename")
    fun searchDB(filename: String): List<Paint>

    @Insert
    fun insert(paint: Paint)

    @Delete
    fun delete(paint: Paint)

    @Delete
    fun delete(paints: Array<Paint>)

    @Update
    fun update(paint: Paint)

}