package com.fontanarsoftlutions.barcoderecords.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fontanarsoftlutions.barcoderecords.R

data class NamedItem(val id: Int, val name: String)

class SimpleNameAdapter(
    private val items: MutableList<NamedItem>,
    private val onEdit: (NamedItem) -> Unit,
    private val onDelete: (NamedItem) -> Unit
) : RecyclerView.Adapter<SimpleNameAdapter.NameViewHolder>() {

    class NameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textName)
        val buttonMenu: ImageButton = view.findViewById(R.id.buttonMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_name, parent, false)
        return NameViewHolder(view)
    }

    override fun onBindViewHolder(holder: NameViewHolder, position: Int) {
        val item = items[position]
        holder.textName.text = item.name

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