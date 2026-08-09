package com.fontanarsoftlutions.barcoderecords

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        findViewById<Button>(R.id.buttonBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.buttonLocations).setOnClickListener {
            startActivity(Intent(this, AdminLocationsActivity::class.java))
        }

        findViewById<Button>(R.id.buttonEquipment).setOnClickListener {
            startActivity(Intent(this, AdminEquipmentActivity::class.java))
        }
    }
}