package com.fontanarsoftlutions.barcoderecords.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_records")
data class BarcodeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val placeId: Int,
    val locationId: Int,
    val equipmentId: Int,
    val barcode: String,
    val timestamp: Long
)