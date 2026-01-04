package com.choreapp.android.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.choreapp.android.R
import com.choreapp.android.models.Chore

class ChoreAdapter(
    private var chores: List<Chore>,
    private val onChoreClick: (Chore) -> Unit
) : RecyclerView.Adapter<ChoreAdapter.ChoreViewHolder>() {

    private var lastPosition = -1

    class ChoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChoreTitle: TextView = view.findViewById(R.id.tvChoreTitle)
        val tvChoreDescription: TextView = view.findViewById(R.id.tvChoreDescription)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvPriorityBadge: TextView = view.findViewById(R.id.tvPriorityBadge)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
        val tvPoints: TextView = view.findViewById(R.id.tvPoints)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chore, parent, false)
        return ChoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        val chore = chores[position]

        holder.tvChoreTitle.text = chore.title
        holder.tvChoreDescription.text = chore.description ?: "No description"
        holder.tvPoints.text = "Points: ${chore.points}"

        // Status badge
        holder.tvStatusBadge.text = chore.status.uppercase()
        holder.tvStatusBadge.setBackgroundColor(getStatusColor(chore.status))

        // Priority badge
        holder.tvPriorityBadge.text = chore.priority.uppercase()
        holder.tvPriorityBadge.setBackgroundColor(getPriorityColor(chore.priority))

        // Due date
        if (!chore.due_date.isNullOrEmpty()) {
            holder.tvDueDate.text = "Due: ${chore.due_date}"
            holder.tvDueDate.visibility = View.VISIBLE
        } else {
            holder.tvDueDate.visibility = View.GONE
        }

        // Location (3p requirement - Location & Maps)
        if (chore.latitude != null && chore.longitude != null) {
            val locationText = chore.location_name ?: "Lat: ${chore.latitude}, Lon: ${chore.longitude}"
            holder.tvLocation.text = "📍 $locationText"
            holder.tvLocation.visibility = View.VISIBLE
        } else {
            holder.tvLocation.visibility = View.GONE
        }

        // Click listener
        holder.itemView.setOnClickListener {
            onChoreClick(chore)
        }

        // Animate item (3p requirement - animations)
        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.item_fade_in)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: ChoreViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    override fun getItemCount(): Int = chores.size

    fun updateChores(newChores: List<Chore>) {
        chores = newChores
        notifyDataSetChanged()
    }

    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> Color.parseColor("#2196F3")
            "in_progress", "in progress" -> Color.parseColor("#FF9800")
            "completed" -> Color.parseColor("#4CAF50")
            else -> Color.parseColor("#9E9E9E")
        }
    }

    private fun getPriorityColor(priority: String): Int {
        return when (priority.lowercase()) {
            "high" -> Color.parseColor("#F44336")
            "medium" -> Color.parseColor("#FF9800")
            "low" -> Color.parseColor("#4CAF50")
            else -> Color.parseColor("#9E9E9E")
        }
    }
}
