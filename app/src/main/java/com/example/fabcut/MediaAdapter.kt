package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class MediaAdapter(
    private val mediaList: MutableList<MediaItem>,
    private val maxLimit: Int, // 1. Added maxLimit here to receive the limit from GalleryActivity
    private val onLongClick: (MediaItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val checkIcon: TextView = itemView.findViewById(R.id.checkIcon)
        val container: FrameLayout = itemView.findViewById(R.id.container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaList[position]

        Glide.with(holder.itemView.context)
            .load(mediaItem.uri)
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCornersTransformation(
                        24,
                        0,
                        RoundedCornersTransformation.CornerType.ALL
                    )
                )
            )
            .into(holder.imageView)

        if (mediaItem.isSelected) {
            holder.checkIcon.visibility = View.VISIBLE
            holder.container.setBackgroundResource(R.drawable.selected_border)
        } else {
            holder.checkIcon.visibility = View.GONE
            holder.container.background = null
        }

        holder.itemView.setOnClickListener {

            // Remove previous selection
            mediaList.forEachIndexed { index, item ->
                if (item.isSelected) {
                    item.isSelected = false
                    notifyItemChanged(index)
                }
            }

            // Select the clicked item
            mediaItem.isSelected = true
            notifyItemChanged(position)

            onSelectionChanged(1)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(mediaItem)
            true
        }
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }
}