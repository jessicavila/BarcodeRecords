package com.fontanarsoftlutions.barcoderecords.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fontanarsoftlutions.barcoderecords.R

data class RecordDisplay(
    val record: BarcodeRecordEntity,
    val locationName: String,
    val equipmentName: String
)

class BarcodeRecordAdapter(
    private val items: List<RecordDisplay>,
    private val onEdit: (RecordDisplay) -> Unit,
    private val onDelete: (RecordDisplay) -> Unit
) : RecyclerView.Adapter<BarcodeRecordAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textLocationEquipment: TextView = view.findViewById(R.id.textLocationEquipment)
        val textBarcode: TextView = view.findViewById(R.id.textBarcode)
        val buttonMenu: ImageButton = view.findViewById(R.id.buttonMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barcode_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = items[position]
        holder.textLocationEquipment.text = "${item.locationName} - ${item.equipmentName}"
        holder.textBarcode.text = item.record.barcode

        holder.buttonMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add("Edit")
            popup.menu.add("Delete")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Edit" -> onEdit(item)
                    "Delete" -> onDelete(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = items.size
}