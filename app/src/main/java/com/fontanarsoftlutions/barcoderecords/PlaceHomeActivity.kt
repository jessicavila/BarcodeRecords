package com.fontanarsoftlutions.barcoderecords

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fontanarsoftlutions.barcoderecords.data.AppDatabase
import com.fontanarsoftlutions.barcoderecords.data.BarcodeRecordAdapter
import com.fontanarsoftlutions.barcoderecords.data.BarcodeRecordEntity
import com.fontanarsoftlutions.barcoderecords.data.NamedItem
import com.fontanarsoftlutions.barcoderecords.data.RecordDisplay
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import com.fontanarsoftlutions.barcoderecords.data.exportAndShareCsv
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaceHomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLACE_ID = "extra_place_id"
        const val EXTRA_PLACE_NAME = "extra_place_name"
    }

    private lateinit var db: AppDatabase
    private var placeId: Int = -1
    private var placeName: String = ""

    private var selectedLocationId: Int = -1  // -1 means "All"
    private var selectedEquipmentId: Int = -1

    private var lastUsedLocationId: Int? = null
    private var lastUsedEquipmentId: Int? = null

    private val recordList = mutableListOf<RecordDisplay>()
    private lateinit var adapter: BarcodeRecordAdapter

    private lateinit var spinnerLocation: Spinner
    private lateinit var spinnerEquipment: Spinner

    private var usedLocations = listOf<NamedItem>()
    private var usedEquipment = listOf<NamedItem>()

    private var suppressSpinnerCallback = false

    private var activeBarcodeField: EditText? = null

    private val scannerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedValue = result.data?.getStringExtra(ScannerActivity.EXTRA_SCANNED_VALUE)
            if (!scannedValue.isNullOrEmpty()) {
                activeBarcodeField?.setText(scannedValue)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_home)

        db = AppDatabase.getInstance(this)
        placeId = intent.getIntExtra(EXTRA_PLACE_ID, -1)
        placeName = intent.getStringExtra(EXTRA_PLACE_NAME) ?: ""

        findViewById<TextView>(R.id.textPlaceName).text = placeName

        findViewById<TextView>(R.id.textChangePlace).setOnClickListener {
            startActivity(Intent(this, PlaceSelectionActivity::class.java))
            finish()
        }

        spinnerLocation = findViewById(R.id.spinnerLocationFilter)
        spinnerEquipment = findViewById(R.id.spinnerEquipmentFilter)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewRecords)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BarcodeRecordAdapter(
            items = recordList,
            onEdit = { record -> showEditRecordDialog(record) },
            onDelete = { record -> showDeleteRecordConfirmation(record) }
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.buttonShowAll).setOnClickListener {
            selectedLocationId = -1
            selectedEquipmentId = -1
            suppressSpinnerCallback = true
            spinnerLocation.setSelection(0)
            spinnerEquipment.setSelection(0)
            suppressSpinnerCallback = false
            loadRecords()
        }
        findViewById<Button>(R.id.buttonExportPlaceCsv).setOnClickListener {
            exportPlaceRecords()
        }

        findViewById<FloatingActionButton>(R.id.fabAddRecord).setOnClickListener {
            showAddRecordDialog()
        }

        loadFiltersAndRecords()
    }

    private fun exportPlaceRecords() {
        lifecycleScope.launch {
            val records = db.appDao().getRecordsForPlaceSortedForExport(placeId)
            val allLocations = db.appDao().getAllLocations().associateBy { it.id }
            val allEquipment = db.appDao().getAllEquipment().associateBy { it.id }

            val place = db.appDao().getAllPlaces().firstOrNull { it.id == placeId }
            val lastModified = place?.lastModified ?: System.currentTimeMillis()
            val dateFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

            val csv = buildString {
                appendLine("$placeName,${dateFormatter.format(Date(lastModified))}")
                appendLine("Location,Equipment,Barcode")
                records.forEach { record ->
                    val locationName = allLocations[record.locationId]?.name ?: "Unknown"
                    val equipmentName = allEquipment[record.equipmentId]?.name ?: "Unknown"
                    appendLine("$locationName,$equipmentName,${record.barcode}")
                }
            }

            val safeFileName = placeName.replace(Regex("[^A-Za-z0-9]"), "_")
            exportAndShareCsv(this@PlaceHomeActivity, "${safeFileName}_barcodes.csv", csv)
        }
    }

    private fun loadFiltersAndRecords() {
        lifecycleScope.launch {
            val usedLoc = db.appDao().getUsedLocationsForPlace(placeId)
            val usedEq = db.appDao().getUsedEquipmentForPlace(placeId)

            usedLocations = listOf(NamedItem(-1, "All Locations")) + usedLoc.map { NamedItem(it.id, it.name) }
            usedEquipment = listOf(NamedItem(-1, "All Equipment")) + usedEq.map { NamedItem(it.id, it.name) }

            val locAdapter = ArrayAdapter(
                this@PlaceHomeActivity,
                android.R.layout.simple_spinner_item,
                usedLocations.map { it.name }
            )
            locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLocation.adapter = locAdapter

            val eqAdapter = ArrayAdapter(
                this@PlaceHomeActivity,
                android.R.layout.simple_spinner_item,
                usedEquipment.map { it.name }
            )
            eqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEquipment.adapter = eqAdapter

            spinnerLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (suppressSpinnerCallback) return
                    selectedLocationId = usedLocations[position].id
                    if (selectedLocationId != -1) lastUsedLocationId = selectedLocationId
                    loadRecords()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            spinnerEquipment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (suppressSpinnerCallback) return
                    selectedEquipmentId = usedEquipment[position].id
                    if (selectedEquipmentId != -1) lastUsedEquipmentId = selectedEquipmentId
                    loadRecords()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            loadRecords()
        }
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            val records = when {
                selectedLocationId != -1 && selectedEquipmentId != -1 ->
                    db.appDao().getRecordsFiltered(placeId, selectedLocationId, selectedEquipmentId)
                selectedLocationId != -1 ->
                    db.appDao().getRecordsFilteredByLocation(placeId, selectedLocationId)
                selectedEquipmentId != -1 ->
                    db.appDao().getRecordsFilteredByEquipment(placeId, selectedEquipmentId)
                else ->
                    db.appDao().getRecordsForPlace(placeId)
            }

            val allLocations = db.appDao().getAllLocations().associateBy { it.id }
            val allEquipment = db.appDao().getAllEquipment().associateBy { it.id }

            recordList.clear()
            recordList.addAll(records.map { record ->
                RecordDisplay(
                    record = record,
                    locationName = allLocations[record.locationId]?.name ?: "Unknown",
                    equipmentName = allEquipment[record.equipmentId]?.name ?: "Unknown"
                )
            })
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddRecordDialog() {
        lifecycleScope.launch {
            val allLocations = db.appDao().getAllLocations()
            val allEquipment = db.appDao().getAllEquipment()

            if (allLocations.isEmpty() || allEquipment.isEmpty()) {
                Toast.makeText(this@PlaceHomeActivity, "Add Locations and Equipment in Admin first", Toast.LENGTH_LONG).show()
                return@launch
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_add_record, null)
            val spinnerDialogLocation = dialogView.findViewById<Spinner>(R.id.spinnerDialogLocation)
            val spinnerDialogEquipment = dialogView.findViewById<Spinner>(R.id.spinnerDialogEquipment)
            val editBarcode = dialogView.findViewById<EditText>(R.id.editBarcode)

            val buttonScan = dialogView.findViewById<Button>(R.id.buttonScan)
            buttonScan.setOnClickListener {
                activeBarcodeField = editBarcode
                scannerLauncher.launch(Intent(this@PlaceHomeActivity, ScannerActivity::class.java))
            }

            val locAdapter = ArrayAdapter(this@PlaceHomeActivity, android.R.layout.simple_spinner_item, allLocations.map { it.name })
            locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDialogLocation.adapter = locAdapter

            val eqAdapter = ArrayAdapter(this@PlaceHomeActivity, android.R.layout.simple_spinner_item, allEquipment.map { it.name })
            eqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDialogEquipment.adapter = eqAdapter

            // Pre-select based on active filter, falling back to last-used
            val preselectLocationId = if (selectedLocationId != -1) selectedLocationId else lastUsedLocationId
            val preselectEquipmentId = if (selectedEquipmentId != -1) selectedEquipmentId else lastUsedEquipmentId

            preselectLocationId?.let { id ->
                val idx = allLocations.indexOfFirst { it.id == id }
                if (idx >= 0) spinnerDialogLocation.setSelection(idx)
            }
            preselectEquipmentId?.let { id ->
                val idx = allEquipment.indexOfFirst { it.id == id }
                if (idx >= 0) spinnerDialogEquipment.setSelection(idx)
            }

            AlertDialog.Builder(this@PlaceHomeActivity)
                .setTitle("Add Barcode Record")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val barcode = editBarcode.text.toString().trim()
                    if (barcode.isNotEmpty()) {
                        val chosenLocation = allLocations[spinnerDialogLocation.selectedItemPosition]
                        val chosenEquipment = allEquipment[spinnerDialogEquipment.selectedItemPosition]
                        lifecycleScope.launch {
                            db.appDao().insertBarcodeRecord(
                                BarcodeRecordEntity(
                                    placeId = placeId,
                                    locationId = chosenLocation.id,
                                    equipmentId = chosenEquipment.id,
                                    barcode = barcode,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            lastUsedLocationId = chosenLocation.id
                            lastUsedEquipmentId = chosenEquipment.id
                            loadFiltersAndRecords()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEditRecordDialog(item: RecordDisplay) {
        lifecycleScope.launch {
            val allLocations = db.appDao().getAllLocations()
            val allEquipment = db.appDao().getAllEquipment()

            val dialogView = layoutInflater.inflate(R.layout.dialog_add_record, null)
            val spinnerDialogLocation = dialogView.findViewById<Spinner>(R.id.spinnerDialogLocation)
            val spinnerDialogEquipment = dialogView.findViewById<Spinner>(R.id.spinnerDialogEquipment)
            val editBarcode = dialogView.findViewById<EditText>(R.id.editBarcode)

            val buttonScan = dialogView.findViewById<Button>(R.id.buttonScan)
            buttonScan.setOnClickListener {
                activeBarcodeField = editBarcode
                scannerLauncher.launch(Intent(this@PlaceHomeActivity, ScannerActivity::class.java))
            }

            val locAdapter = ArrayAdapter(this@PlaceHomeActivity, android.R.layout.simple_spinner_item, allLocations.map { it.name })
            locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDialogLocation.adapter = locAdapter

            val eqAdapter = ArrayAdapter(this@PlaceHomeActivity, android.R.layout.simple_spinner_item, allEquipment.map { it.name })
            eqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDialogEquipment.adapter = eqAdapter

            editBarcode.setText(item.record.barcode)
            val locIdx = allLocations.indexOfFirst { it.id == item.record.locationId }
            if (locIdx >= 0) spinnerDialogLocation.setSelection(locIdx)
            val eqIdx = allEquipment.indexOfFirst { it.id == item.record.equipmentId }
            if (eqIdx >= 0) spinnerDialogEquipment.setSelection(eqIdx)

            AlertDialog.Builder(this@PlaceHomeActivity)
                .setTitle("Edit Barcode Record")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val barcode = editBarcode.text.toString().trim()
                    if (barcode.isNotEmpty()) {
                        val chosenLocation = allLocations[spinnerDialogLocation.selectedItemPosition]
                        val chosenEquipment = allEquipment[spinnerDialogEquipment.selectedItemPosition]
                        lifecycleScope.launch {
                            db.appDao().updateBarcodeRecord(
                                item.record.copy(
                                    locationId = chosenLocation.id,
                                    equipmentId = chosenEquipment.id,
                                    barcode = barcode
                                )
                            )
                            loadFiltersAndRecords()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showDeleteRecordConfirmation(item: RecordDisplay) {
        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage("Delete barcode \"${item.record.barcode}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.appDao().deleteBarcodeRecord(item.record)
                    loadFiltersAndRecords()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}