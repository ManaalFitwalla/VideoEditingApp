package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
class MediaAdapter(
    private val mediaList: List<MediaItem>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)

        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {

        val mediaItem = mediaList[position]

        holder.imageView.setImageURI(
            Uri.parse(mediaItem.uri)
        )
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }
}