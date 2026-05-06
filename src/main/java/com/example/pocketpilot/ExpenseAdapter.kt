package com.example.pocketpilot

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketpilot.data.Expense

class ExpenseAdapter(
    private var list: List<Expense>,
    private val onDelete: (Expense) -> Unit,
    private val onEdit: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = list[position]

        holder.view.findViewById<TextView>(R.id.tvAmount).text = "R ${String.format("%.2f", expense.amount)}"
        holder.view.findViewById<TextView>(R.id.tvCategory).text = expense.category
        holder.view.findViewById<TextView>(R.id.tvDate).text = expense.date

        val img = holder.view.findViewById<ImageView>(R.id.imgExpense)
        expense.imageUri?.let {
            img.setImageURI(Uri.parse(it))
        }

        holder.view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            onDelete(expense)
        }

        holder.view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            onEdit(expense)
        }
    }

    fun getExpenseAt(position: Int): Expense {
        return list[position]
    }

    fun updateData(newList: List<Expense>) {
        list = newList
        notifyDataSetChanged()
    }
}
