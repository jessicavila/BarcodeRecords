package com.fontanarsoftlutions.barcoderecords.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun exportAndShareCsv(context: Context, fileName: String, content: String) {
    val file = File(context.cacheDir, fileName)
    file.writeText(content)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export CSV"))
}