package com.fontanarsoftlutions.barcoderecords.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lastModified: Long = System.currentTimeMillis()
)