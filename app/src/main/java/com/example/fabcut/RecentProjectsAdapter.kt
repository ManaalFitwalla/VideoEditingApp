package com.example.fabcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.example.fabcut.R

class RecentProjectsAdapter(
    private val context: Context,
    private val projectPaths: List<String>
) : RecyclerView.Adapter<RecentProjectsAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.projectCardView)
        val thumbnail: ImageView = view.findViewById(R.id.imgProjectThumbnail)
        val txtLabel: TextView = view.findViewById(R.id.txtProjectLabel)
        val imgTypeIcon: ImageView = view.findViewById(R.id.imgTypeIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        // FIXED: Using the primary class constructor context parameter safely
        val view = LayoutInflater.from(context).inflate(R.layout.item_recent_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val currentPath = projectPaths[position]
        val uri = Uri.parse(currentPath)

        val isVideo = currentPath.contains("video", ignoreCase = true) ||
                currentPath.contains(".mp4", ignoreCase = true)

        holder.txtLabel.text = if (isVideo) "Video Project" else "Image Project"

        holder.imgTypeIcon.setImageResource(
            if (isVideo) android.R.drawable.ic_media_play else android.R.drawable.ic_menu_gallery
        )

        Glide.with(context)
            .load(uri)
            .centerCrop()
            .into(holder.thumbnail)

        holder.cardView.setOnClickListener {
            try {
                val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (isVideo) "video/*" else "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(galleryIntent)
            } catch (e: Exception) {
                val generalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(generalIntent)
            }
        }
    }

    override fun getItemCount(): Int = projectPaths.size
}