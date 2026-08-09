package com.fontanarsoftlutions.barcoderecords

import android.app.AlertDialog
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
import com.fontanarsoftlutions.barcoderecords.data.EquipmentEntity
import com.fontanarsoftlutions.barcoderecords.data.NamedItem
import com.fontanarsoftlutions.barcoderecords.data.SimpleNameAdapter
import com.fontanarsoftlutions.barcoderecords.data.readNamesFromCsv
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import com.fontanarsoftlutions.barcoderecords.data.exportAndShareCsv

class AdminEquipmentActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_admin_equipment)

        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEquipment)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SimpleNameAdapter(
            items = nameList,
            onEdit = { item -> showEditDialog(item) },
            onDelete = { item -> showDeleteConfirmation(item) }
        )
        recyclerView.adapter = adapter

        loadEquipment()

        findViewById<Button>(R.id.buttonBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.buttonImportCsv).setOnClickListener {
            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }

        findViewById<Button>(R.id.buttonClearAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All Equipment")
                .setMessage("This will delete all equipment. This cannot be undone. Continue?")
                .setPositiveButton("Clear All") { _, _ ->
                    lifecycleScope.launch {
                        db.appDao().deleteAllEquipment()
                        loadEquipment()
                        Toast.makeText(this@AdminEquipmentActivity, "All equipment cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<FloatingActionButton>(R.id.fabAddEquipment).setOnClickListener {
            showAddDialog()
        }

        findViewById<Button>(R.id.buttonExportCsv).setOnClickListener {
            lifecycleScope.launch {
                val equipment = db.appDao().getAllEquipment()
                val csv = buildString {
                    appendLine("Equipment")
                    equipment.sortedBy { it.name }.forEach { appendLine(it.name) }
                }
                exportAndShareCsv(this@AdminEquipmentActivity, "equipment.csv", csv)
            }
        }
    }

    private fun loadEquipment() {
        lifecycleScope.launch {
            val equipment = db.appDao().getAllEquipment()
            nameList.clear()
            nameList.addAll(equipment.map { NamedItem(it.id, it.name) })
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = "Equipment name"

        AlertDialog.Builder(this)
            .setTitle("Add Equipment")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val existing = db.appDao().getEquipmentByName(name)
                        if (existing == null) {
                            db.appDao().insertEquipment(EquipmentEntity(name = name))
                            loadEquipment()
                        } else {
                            Toast.makeText(this@AdminEquipmentActivity, "Already exists", Toast.LENGTH_SHORT).show()
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
            .setTitle("Edit Equipment")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.appDao().updateEquipment(EquipmentEntity(id = item.id, name = newName))
                        loadEquipment()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(item: NamedItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Equipment")
            .setMessage("Delete \"${item.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.appDao().deleteEquipment(EquipmentEntity(id = item.id, name = item.name))
                    loadEquipment()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importCsv(uri: Uri) {
        lifecycleScope.launch {
            val names = readNamesFromCsv(this@AdminEquipmentActivity, uri)
            var addedCount = 0
            var skippedCount = 0

            names.forEach { name ->
                val existing = db.appDao().getEquipmentByName(name)
                if (existing == null) {
                    db.appDao().insertEquipment(EquipmentEntity(name = name))
                    addedCount++
                } else {
                    skippedCount++
                }
            }

            loadEquipment()
            Toast.makeText(
                this@AdminEquipmentActivity,
                "Imported $addedCount new, skipped $skippedCount existing",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}