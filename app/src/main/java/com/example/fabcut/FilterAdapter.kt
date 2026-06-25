package com.example.fabcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FilterAdapter(
    private val filterList: List<FilterItem>,
    private val onFilterClick: (FilterItem) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var selectedPosition = 0

    class FilterViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtFilter: TextView =
            itemView.findViewById(R.id.txtFilter)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FilterViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_filter,
                parent,
                false
            )

        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: FilterViewHolder,
        position: Int
    ) {

        val filter = filterList[position]

        holder.txtFilter.text = filter.name

        if (position == selectedPosition) {

            holder.txtFilter.setBackgroundResource(
                R.drawable.selected_filter_bg
            )

        } else {

            holder.txtFilter.setBackgroundResource(
                R.drawable.filter_bg
            )
        }

        holder.itemView.setOnClickListener {

            val adapterPosition =
                holder.adapterPosition

            if (
                adapterPosition ==
                RecyclerView.NO_POSITION
            ) {
                return@setOnClickListener
            }

            val oldPosition =
                selectedPosition

            selectedPosition =
                adapterPosition

            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            onFilterClick(
                filterList[adapterPosition]
            )
        }
    }

    override fun getItemCount(): Int {

        return filterList.size
    }
}