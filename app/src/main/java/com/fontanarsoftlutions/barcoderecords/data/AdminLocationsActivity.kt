package com.fontanarsoftlutions.barcoderecords

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fontanarsoftlutions.barcoderecords.data.AppDatabase
import com.fontanarsoftlutions.barcoderecords.data.LocationEntity
import com.fontanarsoftlutions.barcoderecords.data.NamedItem
import com.fontanarsoftlutions.barcoderecords.data.SimpleNameAdapter
import com.fontanarsoftlutions.barcoderecords.data.readNamesFromCsv
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import com.fontanarsoftlutions.barcoderecords.data.exportAndShareCsv

class AdminLocationsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val nameList = mutableListOf<NamedItem>()
    private lateinit var adapter: SimpleNameAdapter

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) importCsv(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_locations)

        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLocations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SimpleNameAdapter(
            items = nameList,
            onEdit = { item -> showEditDialog(item) },
            onDelete = { item -> showDeleteConfirmation(item) }
        )
        recyclerView.adapter = adapter

        loadLocations()

        findViewById<Button>(R.id.buttonBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.buttonImportCsv).setOnClickListener {
            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }

        findViewById<FloatingActionButton>(R.id.fabAddLocation).setOnClickListener {
            showAddDialog()
        }

        findViewById<Button>(R.id.buttonClearAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All Locations")
                .setMessage("This will delete all locations. This cannot be undone. Continue?")
                .setPositiveButton("Clear All") { _, _ ->
                    lifecycleScope.launch {
                        db.appDao().deleteAllLocations()
                        loadLocations()
                        Toast.makeText(this@AdminLocationsActivity, "All locations cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        findViewById<Button>(R.id.buttonExportCsv).setOnClickListener {
            lifecycleScope.launch {
                val locations = db.appDao().getAllLocations()
                val csv = buildString {
                    appendLine("Location")
                    locations.sortedBy { it.name }.forEach { appendLine(it.name) }
                }
                exportAndShareCsv(this@AdminLocationsActivity, "locations.csv", csv)
            }
        }
    }

    private fun loadLocations() {
        lifecycleScope.launch {
            val locations = db.appDao().getAllLocations()
            nameList.clear()
            nameList.addAll(locations.map { NamedItem(it.id, it.name) })
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = "Location name"

        AlertDialog.Builder(this)
            .setTitle("Add Location")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val existing = db.appDao().getLocationByName(name)
                        if (existing == null) {
                            db.appDao().insertLocation(LocationEntity(name = name))
                            loadLocations()
                        } else {
                            Toast.makeText(this@AdminLocationsActivity, "Already exists", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(item: NamedItem) {
        val input = EditText(this)
        input.setText(item.name)

        AlertDialog.Builder(this)
            .setTitle("Edit Location")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.appDao().updateLocation(LocationEntity(id = item.id, name = newName))
                        loadLocations()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(item: NamedItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Location")
            .setMessage("Delete \"${item.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.appDao().deleteLocation(LocationEntity(id = item.id, name = item.name))
                    loadLocations()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importCsv(uri: Uri) {
        lifecycleScope.launch {
            val names = readNamesFromCsv(this@AdminLocationsActivity, uri)
            var addedCount = 0
            var skippedCount = 0

            names.forEach { name ->
                val existing = db.appDao().getLocationByName(name)
                if (existing == null) {
                    db.appDao().insertLocation(LocationEntity(name = name))
                    addedCount++
                } else {
                    skippedCount++
                }
            }

            loadLocations()
            Toast.makeText(
                this@AdminLocationsActivity,
                "Imported $addedCount new, skipped $skippedCount existing",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}