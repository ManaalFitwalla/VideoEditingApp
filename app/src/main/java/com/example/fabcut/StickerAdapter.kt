package com.example.fabcut

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class StickerAdapter(
    private val stickers: List<StickerItem>,
    private val onClick: (StickerItem) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerHolder>() {

    class StickerHolder(view: android.view.View) :
        RecyclerView.ViewHolder(view) {

        val image: ImageView =
            view.findViewById(R.id.imgSticker)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StickerHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker, parent, false)

        return StickerHolder(view)
    }

    override fun onBindViewHolder(
        holder: StickerHolder,
        position: Int
    ) {

        val sticker = stickers[position]

        holder.image.setImageResource(sticker.sticker)

        holder.itemView.setOnClickListener {
            onClick(sticker)
        }
    }

    override fun getItemCount(): Int {
        return stickers.size
    }
}