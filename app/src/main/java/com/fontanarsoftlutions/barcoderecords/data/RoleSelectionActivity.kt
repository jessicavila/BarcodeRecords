package com.fontanarsoftlutions.barcoderecords

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RoleSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        findViewById<Button>(R.id.buttonRecordBarcode).setOnClickListener {
            startActivity(Intent(this, PlaceSelectionActivity::class.java))
        }

        findViewById<Button>(R.id.buttonAdmin).setOnClickListener {
            startActivity(Intent(this, AdminHomeActivity::class.java))
        }
    }
}