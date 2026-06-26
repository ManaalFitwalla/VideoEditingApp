package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class MediaAdapter(
    private val mediaList: MutableList<MediaItem>,
    private val onLongClick: (MediaItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
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

            holder.container.setBackgroundResource(
                R.drawable.selected_border
            )

        } else {

            holder.checkIcon.visibility = View.GONE

            holder.container.background = null
        }

        holder.itemView.setOnClickListener {

            val maxLimit =
                if (mediaItem.isVideo) 10 else 20

            val selectedCount =
                mediaList.count {
                    it.isSelected &&
                            it.isVideo == mediaItem.isVideo
                }

            if (
                !mediaItem.isSelected &&
                selectedCount >= maxLimit
            ) {

                Toast.makeText(
                    holder.itemView.context,
                    if (mediaItem.isVideo)
                        "You can select up to 10 videos at a time."
                    else
                        "You can select up to 20 photos at a time.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            mediaItem.isSelected =
                !mediaItem.isSelected

            notifyItemChanged(position)

            val totalSelected =
                mediaList.count { it.isSelected }

            onSelectionChanged(totalSelected)
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