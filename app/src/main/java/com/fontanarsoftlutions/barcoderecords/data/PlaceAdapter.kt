package com.fontanarsoftlutions.barcoderecords.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fontanarsoftlutions.barcoderecords.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaceAdapter(
    private val items: List<PlaceEntity>,
    private val onSelect: (PlaceEntity) -> Unit,
    private val onDelete: (PlaceEntity) -> Unit,
    private val onExportCsv: (PlaceEntity) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textPlaceName: TextView = view.findViewById(R.id.textPlaceName)
        val textPlaceDate: TextView = view.findViewById(R.id.textPlaceDate)
        val buttonMenu: ImageButton = view.findViewById(R.id.buttonMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val item = items[position]
        holder.textPlaceName.text = item.name

        val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        holder.textPlaceDate.text = formatter.format(Date(item.lastModified))

        holder.buttonMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add("Select")
            popup.menu.add("Delete")
            popup.menu.add("Export to CSV")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Select" -> onSelect(item)
                    "Delete" -> onDelete(item)
                    "Export to CSV" -> onExportCsv(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = items.size
}