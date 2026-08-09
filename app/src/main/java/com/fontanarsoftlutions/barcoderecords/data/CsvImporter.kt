package com.fontanarsoftlutions.barcoderecords.data

import android.content.Context
import android.net.Uri

// Reads a single-column CSV (with a header row) and returns the list of names found.
// Skips the header, skips blank lines, trims whitespace.
fun readNamesFromCsv(context: Context, uri: Uri): List<String> {
    val names = mutableListOf<String>()
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
        lines.drop(1) // skip header row
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { names.add(it) }
    }
    return names
}