package com.abdulkadirkara.paint

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "paints")
data class Paint(
    var imageName: String,
    var imagePath: String
) {
    @PrimaryKey(autoGenerate = true)
    var id=0

    @Ignore
    var isChecked=false
}