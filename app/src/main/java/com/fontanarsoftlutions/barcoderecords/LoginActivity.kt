package com.fontanarsoftlutions.barcoderecords

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private val masterCode = "6911"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val codeField = findViewById<EditText>(R.id.editCode)

        // TODO: QA convenience only — remove this line before building the release APK
        codeField.setText(masterCode)

        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            val entered = codeField.text.toString()
            if (entered == masterCode) {
                startActivity(Intent(this, RoleSelectionActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Incorrect code", Toast.LENGTH_SHORT).show()
                codeField.text.clear()
            }
        }
    }
}