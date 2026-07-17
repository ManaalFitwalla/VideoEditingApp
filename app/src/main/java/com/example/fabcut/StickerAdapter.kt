package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StickerAdapter(
    private val stickers: List<String>,
    private val onStickerClick: (String) -> Unit
) : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtSticker: TextView = view.findViewById(R.id.txtSticker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sticker = stickers[position]
        holder.txtSticker.text = sticker

        // Ensure absolutely full opacity on rendering item views
        holder.txtSticker.alpha = 1.0f
        holder.itemView.alpha = 1.0f

        holder.itemView.setOnClickListener { onStickerClick(sticker) }
    }

    override fun getItemCount(): Int = stickers.size
}