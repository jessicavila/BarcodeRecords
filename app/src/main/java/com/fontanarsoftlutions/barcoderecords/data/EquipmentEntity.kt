package com.fontanarsoftlutions.barcoderecords.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)