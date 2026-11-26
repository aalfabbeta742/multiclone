package com.multiclone.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CloneAdapter(
    private val items: List<CloneItem>,
    private val onClick: (CloneItem) -> Unit,
    private val onLongClick: (CloneItem, View) -> Unit
) : RecyclerView.Adapter<CloneAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val name: TextView = v.findViewById(R.id.name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_clone, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.name.text = item.label
        try {
            h.icon.setImageDrawable(h.itemView.context.packageManager.getApplicationIcon(item.pkg))
        } catch (e: Exception) {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        h.itemView.setOnClickListener { onClick(item) }
        h.itemView.setOnLongClickListener { onLongClick(item, it); true }
    }

    override fun getItemCount() = items.size
}
