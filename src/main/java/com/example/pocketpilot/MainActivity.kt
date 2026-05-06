package com.example.pocketpilot

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketpilot.data.AppDatabase
import com.example.pocketpilot.data.Expense
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.util.Calendar

import androidx.core.widget.NestedScrollView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ExpenseAdapter
    private lateinit var db: AppDatabase
    private lateinit var pieChart: PieChart
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnAdd: ExtendedFloatingActionButton
    
    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        pieChart = findViewById(R.id.pieChart)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnAdd = findViewById(R.id.btnAddExpense)
        val btnStart = findViewById<Button>(R.id.btnStartDate)
        val btnEnd = findViewById<Button>(R.id.btnEndDate)
        val btnFilter = findViewById<Button>(R.id.btnFilter)
        val btnTotals = findViewById<Button>(R.id.btnTotals)
        val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)

        db = AppDatabase.getDatabase(this)

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Add Scrolling Feature: Shrink FAB on scroll
        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 12 && btnAdd.isExtended) {
                btnAdd.shrink()
            } else if (scrollY < oldScrollY - 12 && !btnAdd.isExtended) {
                btnAdd.extend()
            }
        })

        btnStart.setOnClickListener { showDatePicker { date -> 
            startDate = date
            btnStart.text = date
        }}

        btnEnd.setOnClickListener { showDatePicker { date -> 
            endDate = date
            btnEnd.text = date
        }}

        btnFilter.setOnClickListener {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Select both dates", Toast.LENGTH_SHORT).show()
            } else {
                loadFilteredData()
            }
        }

        btnTotals.setOnClickListener {
            startDate = ""
            endDate = ""
            btnStart.text = getString(R.string.start)
            btnEnd.text = getString(R.string.end)
            loadData()
        }

        adapter = ExpenseAdapter(emptyList(),
            onDelete = { expense -> showDeleteDialog(expense) },
            onEdit = {
                Toast.makeText(this, "Edit coming", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Add Swipe to Delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val expense = adapter.getExpenseAt(position)
                showDeleteDialog(expense) {
                    adapter.notifyItemChanged(position) // Reset swipe if cancelled
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun showDeleteDialog(expense: Expense, onCancel: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle("Remove Expense")
            .setMessage("Are you sure you want to delete this ${expense.category} expense of R ${String.format(java.util.Locale.US, "%.2f", expense.amount)}?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    db.expenseDao().deleteExpense(expense)
                    if (startDate.isNotEmpty() && endDate.isNotEmpty()) loadFilteredData() else loadData()
                    Toast.makeText(this@MainActivity, "Expense removed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener { onCancel?.invoke() }
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val date = String.format("%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadFilteredData() {
        lifecycleScope.launch {
            val data = db.expenseDao().getExpensesBetween(startDate, endDate)
            adapter.updateData(data)
            calculateAndDisplayTotal(data)
            updatePieChartWithData(data)
        }
    }

    private fun calculateAndDisplayTotal(data: List<Expense>) {
        val total = data.sumOf { it.amount }
        tvTotalAmount.text = String.format(java.util.Locale.US, "R %.2f", total)
    }

    override fun onResume() {
        super.onResume()
        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            loadFilteredData()
        } else {
            loadData()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val data = db.expenseDao().getAllExpenses()
            adapter.updateData(data)
            calculateAndDisplayTotal(data)
            updatePieChart()
        }
    }

    private fun updatePieChart() {
        lifecycleScope.launch {
            val totals = db.expenseDao().getCategoryTotals()
            updatePieUI(totals.map { PieEntry(it.total.toFloat(), it.category) })
        }
    }

    private fun updatePieChartWithData(data: List<Expense>) {
        val categoryTotals = data.groupBy { it.category }
            .map { (cat, list) -> PieEntry(list.sumOf { it.amount }.toFloat(), cat) }
        updatePieUI(categoryTotals)
    }

    private fun updatePieUI(entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }
        
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            "#1A237E".toColorInt(),
            "#00B8D4".toColorInt(),
            "#00E676".toColorInt(),
            "#FFD600".toColorInt(),
            "#FF5252".toColorInt()
        )
        dataSet.valueTextColor = android.graphics.Color.WHITE
        dataSet.valueTextSize = 12f
        
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.setEntryLabelColor(android.graphics.Color.WHITE)
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.isRotationEnabled = false // Disable rotation to let scrolling work
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
