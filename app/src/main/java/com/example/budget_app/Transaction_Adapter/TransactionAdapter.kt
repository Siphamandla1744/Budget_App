package com.example.budget_app.Transaction_Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.transactions

class TransactionAdapter(
    private var transactions: List<transactions>,
    private val onTransactionClick: (transactions) -> Unit,
    private val onTransactionLongClick: (transactions) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvTimeStamp: TextView = itemView.findViewById(R.id.tvTimeStamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.tvTitle.text = transaction.transaction_name
        holder.tvCategory.text = transaction.category.category_name
        holder.tvAmount.text = "R ${String.format("%.2f", transaction.transaction_amamount)}"
        holder.tvTimeStamp.text = transaction.transaction_date

        holder.itemView.setOnClickListener { onTransactionClick(transaction) }
        holder.itemView.setOnLongClickListener {
            onTransactionLongClick(transaction)
            true
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateList(newList: List<transactions>) {
        transactions = newList
        notifyDataSetChanged()
    }
}