package com.example.pocketpilot

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pocketpilot.data.AppDatabase
import com.example.pocketpilot.data.Expense
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private lateinit var db: AppDatabase
    private lateinit var imagePreview: ImageView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            imagePreview.setImageURI(it)
            imagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        db = AppDatabase.getDatabase(this)
        imagePreview = findViewById(R.id.imagePreview)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val cardImage = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardImage)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        cardImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnSave.setOnClickListener {
            val amountStr = findViewById<EditText>(R.id.etAmount).text.toString().replace(",", ".")
            val category = findViewById<EditText>(R.id.etCategory).text.toString()
            val desc = findViewById<EditText>(R.id.etDescription).text.toString()

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val expense = Expense(
                amount = amount,
                category = category,
                description = desc,
                date = currentDate,
                imageUri = imageUri?.toString(),
            )

            lifecycleScope.launch {
                db.expenseDao().insert(expense)
                finish()
            }
        }
    }
}
