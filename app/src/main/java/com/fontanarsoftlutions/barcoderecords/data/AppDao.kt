package com.fontanarsoftlutions.barcoderecords.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppDao {

    @Query("SELECT * FROM places ORDER BY lastModified DESC")
    suspend fun getAllPlaces(): List<PlaceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlace(place: PlaceEntity): Long

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Query("SELECT * FROM locations ORDER BY name ASC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE name = :name LIMIT 1")
    suspend fun getLocationByName(name: String): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocation(location: LocationEntity): Long

    @Query("SELECT * FROM equipment ORDER BY name ASC")
    suspend fun getAllEquipment(): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE name = :name LIMIT 1")
    suspend fun getEquipmentByName(name: String): EquipmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEquipment(equipment: EquipmentEntity): Long

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()

    @Query("DELETE FROM equipment")
    suspend fun deleteAllEquipment()

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)

    @Delete
    suspend fun deleteEquipment(equipment: EquipmentEntity)

    @Query("DELETE FROM barcode_records WHERE placeId = :placeId")
    suspend fun deleteBarcodeRecordsForPlace(placeId: Int)

    @Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("""
    SELECT * FROM barcode_records 
    WHERE placeId = :placeId 
    ORDER BY timestamp DESC
""")
    suspend fun getRecordsForPlace(placeId: Int): List<BarcodeRecordEntity>

    @Query("""
    SELECT * FROM barcode_records 
    WHERE placeId = :placeId AND locationId = :locationId AND equipmentId = :equipmentId
    ORDER BY timestamp DESC
""")
    suspend fun getRecordsFiltered(placeId: Int, locationId: Int, equipmentId: Int): List<BarcodeRecordEntity>

    @Query("""
    SELECT * FROM barcode_records 
    WHERE placeId = :placeId AND locationId = :locationId
    ORDER BY timestamp DESC
""")
    suspend fun getRecordsFilteredByLocation(placeId: Int, locationId: Int): List<BarcodeRecordEntity>

    @Query("""
    SELECT * FROM barcode_records 
    WHERE placeId = :placeId AND equipmentId = :equipmentId
    ORDER BY timestamp DESC
""")
    suspend fun getRecordsFilteredByEquipment(placeId: Int, equipmentId: Int): List<BarcodeRecordEntity>

    @Insert
    suspend fun insertBarcodeRecord(record: BarcodeRecordEntity): Long

    @Update
    suspend fun updateBarcodeRecord(record: BarcodeRecordEntity)

    @Delete
    suspend fun deleteBarcodeRecord(record: BarcodeRecordEntity)

    @Query("""
    SELECT DISTINCT l.* FROM locations l
    INNER JOIN barcode_records br ON br.locationId = l.id
    WHERE br.placeId = :placeId
    ORDER BY l.name ASC
""")
    suspend fun getUsedLocationsForPlace(placeId: Int): List<LocationEntity>

    @Query("""
    SELECT DISTINCT e.* FROM equipment e
    INNER JOIN barcode_records br ON br.equipmentId = e.id
    WHERE br.placeId = :placeId
    ORDER BY e.name ASC
""")
    suspend fun getUsedEquipmentForPlace(placeId: Int): List<EquipmentEntity>

    @Query("""
    SELECT br.* FROM barcode_records br
    INNER JOIN locations l ON br.locationId = l.id
    INNER JOIN equipment e ON br.equipmentId = e.id
    WHERE br.placeId = :placeId
    ORDER BY l.name ASC, e.name ASC
""")
    suspend fun getRecordsForPlaceSortedForExport(placeId: Int): List<BarcodeRecordEntity>
}