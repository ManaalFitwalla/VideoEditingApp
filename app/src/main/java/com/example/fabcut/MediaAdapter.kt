package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class MediaAdapter(
    private val mediaList: MutableList<MediaItem>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView =
            itemView.findViewById(R.id.imageView)

        val checkIcon: ImageView =
            itemView.findViewById(R.id.checkIcon)

        val container: FrameLayout =
            itemView.findViewById(R.id.container)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MediaViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)

        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int
    ) {

        val mediaItem = mediaList[position]

        holder.imageView.setImageURI(mediaItem.uri)

        if (mediaItem.isSelected) {

            holder.checkIcon.visibility = View.VISIBLE

            holder.container.setBackgroundResource(
                R.drawable.selected_border
            )

        } else {

            holder.checkIcon.visibility = View.GONE

            holder.container.background = null
        }

        holder.itemView.setOnClickListener {

            mediaItem.isSelected = !mediaItem.isSelected

            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }
}