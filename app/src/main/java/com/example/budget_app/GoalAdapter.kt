package com.example.budget_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.model.Goal

class GoalAdapter(
    private var goals: List<Goal>,
    private val onGoalClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        val tvTargetDate: TextView = itemView.findViewById(R.id.tvTargetDate)
        val tvCurrentAmount: TextView = itemView.findViewById(R.id.tvCurrentAmount)
        val tvTargetAmount: TextView = itemView.findViewById(R.id.tvTargetAmount)
        val pbGoalProgress: ProgressBar = itemView.findViewById(R.id.pbGoalProgress)
        val tvProgressPercent: TextView = itemView.findViewById(R.id.tvProgressPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvGoalName.text = goal.name
        holder.tvTargetDate.text = "Target Date: ${goal.targetDate}"
        holder.tvCurrentAmount.text = "R ${String.format("%.2f", goal.currentAmount)}"
        holder.tvTargetAmount.text = "Target: R ${String.format("%.2f", goal.targetAmount)}"

        val progress = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100).toInt()
        } else 0

        holder.pbGoalProgress.progress = progress.coerceIn(0, 100)
        holder.tvProgressPercent.text = "$progress%"

        holder.itemView.setOnClickListener { onGoalClick(goal) }
    }

    override fun getItemCount(): Int = goals.size

    fun updateList(newList: List<Goal>) {
        goals = newList
        notifyDataSetChanged()
    }
}