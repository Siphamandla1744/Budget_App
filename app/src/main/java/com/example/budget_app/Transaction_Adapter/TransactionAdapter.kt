package com.example.budget_app.transaction_adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.transactions
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<transactions>,
    private val onTransactionClick: (transactions) -> Unit,
    private val onTransactionLongClick: (transactions) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvTimeStamp: TextView = itemView.findViewById(R.id.tvTimeStamp)
        val ivReceiptIcon: ImageView = itemView.findViewById(R.id.ivReceiptIcon)
        val vCategoryIndicator: View = itemView.findViewById(R.id.vCategoryIndicator)
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
        
        val isExpense = transaction.category.type == "Expense"
        val prefix = if (isExpense) "-" else "+"
        holder.tvAmount.text = String.format(Locale.getDefault(), "%sR %.2f", prefix, transaction.transaction_amamount)
        
        val context = holder.itemView.context
        val amountColor = if (isExpense) Color.parseColor("#FF5252") else Color.parseColor("#00E5FF")
        holder.tvAmount.setTextColor(amountColor)
        
        try {
            holder.vCategoryIndicator.setBackgroundColor(Color.parseColor(transaction.category.color))
        } catch (e: Exception) {
            holder.vCategoryIndicator.setBackgroundColor(amountColor)
        }
        
        holder.tvTimeStamp.text = transaction.transaction_date

        if (!transaction.receiptUrl.isNullOrEmpty()) {
            holder.ivReceiptIcon.visibility = View.VISIBLE
            holder.ivReceiptIcon.setOnClickListener {
                showReceiptDialog(holder.itemView.context, transaction)
            }
        } else {
            holder.ivReceiptIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onTransactionClick(transaction) }
        holder.itemView.setOnLongClickListener {
            onTransactionLongClick(transaction)
            true
        }
    }

    private fun showReceiptDialog(context: android.content.Context, transaction: transactions) {
        val imageView = ImageView(context)
        imageView.setPadding(32, 32, 32, 32)
        
        val receiptData = transaction.receiptUrl
        if (!receiptData.isNullOrEmpty()) {
            if (receiptData.startsWith("http")) {
                // Handle legacy URLs
                Glide.with(context).load(receiptData).into(imageView)
            } else {
                // Handle Base64 encoded images
                try {
                    val imageBytes = Base64.decode(receiptData, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    imageView.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Receipt Preview")
            .setView(imageView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Delete Receipt") { _, _ ->
                deleteReceiptOnly(context, transaction)
            }
            .show()
    }

    private fun deleteReceiptOnly(context: android.content.Context, transaction: transactions) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        AlertDialog.Builder(context)
            .setTitle("Delete Receipt")
            .setMessage("Are you sure you want to remove the receipt image? The transaction will remain.")
            .setPositiveButton("Delete") { _, _ ->
                // Just remove the receipt data from the Realtime Database
                val dbRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId).child("transactions").child(transaction.transaction_id)
                
                dbRef.child("receiptUrl").removeValue().addOnCompleteListener { dbTask ->
                    if (dbTask.isSuccessful) {
                        Toast.makeText(context, "Receipt removed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update transaction", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount(): Int = transactions.size

    fun updateList(newList: List<transactions>) {
        transactions = newList
        notifyDataSetChanged()
    }
}
