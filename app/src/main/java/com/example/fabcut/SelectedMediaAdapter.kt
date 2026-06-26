package com.example.fabcut

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SelectedMediaAdapter(
    private val mediaList: ArrayList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SelectedMediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgSelected: ImageView =
            itemView.findViewById(R.id.imgSelected)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MediaViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_selected_media,
                    parent,
                    false
                )

        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int
    ) {

        val mediaUri =
            mediaList[position]

        Glide.with(holder.itemView.context)
            .load(Uri.parse(mediaUri))
            .centerCrop()
            .into(holder.imgSelected)

        holder.itemView.setOnClickListener {

            onItemClick(mediaUri)
        }
    }

    override fun getItemCount(): Int {

        return mediaList.size
    }
}