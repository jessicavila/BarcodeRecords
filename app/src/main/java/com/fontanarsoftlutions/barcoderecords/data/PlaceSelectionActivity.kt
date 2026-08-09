package com.fontanarsoftlutions.barcoderecords

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fontanarsoftlutions.barcoderecords.data.AppDatabase
import com.fontanarsoftlutions.barcoderecords.data.PlaceAdapter
import com.fontanarsoftlutions.barcoderecords.data.PlaceEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import android.content.Intent
import com.fontanarsoftlutions.barcoderecords.data.exportAndShareCsv
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaceSelectionActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: PlaceAdapter
    private val placeList = mutableListOf<PlaceEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_selection)

        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPlaces)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PlaceAdapter(
            items = placeList,
            onSelect = { place ->
                val intent = Intent(this, PlaceHomeActivity::class.java)
                intent.putExtra(PlaceHomeActivity.EXTRA_PLACE_ID, place.id)
                intent.putExtra(PlaceHomeActivity.EXTRA_PLACE_NAME, place.name)
                startActivity(intent)
            },
            onDelete = { place -> showDeletePlaceConfirmation(place) },
            onExportCsv = { place -> exportPlaceToCsv(place) }
        )
        recyclerView.adapter = adapter

        loadPlaces()

        findViewById<FloatingActionButton>(R.id.fabAddPlace).setOnClickListener {
            showAddPlaceDialog()
        }
    }

    private fun loadPlaces() {
        lifecycleScope.launch {
            val places = db.appDao().getAllPlaces()
            placeList.clear()
            placeList.addAll(places)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddPlaceDialog() {
        val input = EditText(this)
        input.hint = "Place name"

        AlertDialog.Builder(this)
            .setTitle("Add Place")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.appDao().insertPlace(PlaceEntity(name = name))
                        loadPlaces()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeletePlaceConfirmation(place: PlaceEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Place")
            .setMessage("Delete \"${place.name}\" and all its recorded barcodes? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.appDao().deleteBarcodeRecordsForPlace(place.id)
                    db.appDao().deletePlace(place)
                    loadPlaces()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportPlaceToCsv(place: PlaceEntity) {
        lifecycleScope.launch {
            val records = db.appDao().getRecordsForPlaceSortedForExport(place.id)
            val allLocations = db.appDao().getAllLocations().associateBy { it.id }
            val allEquipment = db.appDao().getAllEquipment().associateBy { it.id }

            val dateFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

            val csv = buildString {
                appendLine("${place.name},${dateFormatter.format(Date(place.lastModified))}")
                appendLine("Location,Equipment,Barcode")
                records.forEach { record ->
                    val locationName = allLocations[record.locationId]?.name ?: "Unknown"
                    val equipmentName = allEquipment[record.equipmentId]?.name ?: "Unknown"
                    appendLine("$locationName,$equipmentName,${record.barcode}")
                }
            }

            val safeFileName = place.name.replace(Regex("[^A-Za-z0-9]"), "_")
            exportAndShareCsv(this@PlaceSelectionActivity, "${safeFileName}_barcodes.csv", csv)
        }
    }
}