package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: List<CategoryItem>,
    private val onClick: (CategoryItem) -> Unit

    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val category = categories[position]

        holder.icon.setImageResource(category.icon)

        holder.itemView.setOnClickListener {
            onClick(category)
        }
    }
}