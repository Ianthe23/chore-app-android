package com.choreapp.android.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.choreapp.android.R
import com.choreapp.android.models.Chore

class ChoreAdapter(
    private var chores: List<Chore>,
    private val onChoreClick: (Chore) -> Unit
) : RecyclerView.Adapter<ChoreAdapter.ChoreViewHolder>() {

    class ChoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChoreTitle: TextView = view.findViewById(R.id.tvChoreTitle)
        val tvChoreDescription: TextView = view.findViewById(R.id.tvChoreDescription)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvPriorityBadge: TextView = view.findViewById(R.id.tvPriorityBadge)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
        val tvPoints: TextView = view.findViewById(R.id.tvPoints)
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

        // Click listener
        holder.itemView.setOnClickListener {
            onChoreClick(chore)
        }
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
