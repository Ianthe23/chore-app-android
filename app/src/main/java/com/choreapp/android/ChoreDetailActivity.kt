package com.choreapp.android

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.choreapp.android.api.RetrofitClient
import com.choreapp.android.databinding.ActivityChoreDetailBinding
import com.choreapp.android.models.Chore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChoreDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChoreDetailBinding
    private var choreId: Int? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoreDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupDropdowns()
        loadChoreData()
        setupListeners()
    }

    private fun setupDropdowns() {
        // Status dropdown
        val statuses = arrayOf("pending", "in_progress", "completed")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actvStatus.setAdapter(statusAdapter)

        // Priority dropdown
        val priorities = arrayOf("low", "medium", "high")
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorities)
        binding.actvPriority.setAdapter(priorityAdapter)
    }

    private fun loadChoreData() {
        choreId = intent.getIntExtra("chore_id", -1)

        if (choreId == -1) {
            // New chore
            isEditMode = true
            supportActionBar?.title = "New Chore"
            enableEditMode()

            // Set default values
            binding.actvStatus.setText("pending", false)
            binding.actvPriority.setText("medium", false)
            binding.etPoints.setText("0")

            binding.btnDelete.visibility = View.GONE
        } else {
            // Existing chore
            supportActionBar?.title = "Chore Details"
            loadExistingChore()
        }
    }

    private fun loadExistingChore() {
        // Load from intent extras
        binding.etTitle.setText(intent.getStringExtra("chore_title") ?: "")
        binding.etDescription.setText(intent.getStringExtra("chore_description") ?: "")
        binding.actvStatus.setText(intent.getStringExtra("chore_status") ?: "pending", false)
        binding.actvPriority.setText(intent.getStringExtra("chore_priority") ?: "medium", false)
        binding.etDueDate.setText(intent.getStringExtra("chore_due_date") ?: "")
        binding.etPoints.setText(intent.getIntExtra("chore_points", 0).toString())

        // Enable editing by default
        enableEditMode()
    }

    private fun setupListeners() {
        // Date picker for due date
        binding.etDueDate.setOnClickListener {
            showDatePicker()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveChore()
            }
        }

        // Delete button
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        // Enable editing when user starts typing
        binding.etTitle.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isEditMode) {
                enableEditMode()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Parse existing date if available
        val currentDate = binding.etDueDate.text.toString()
        if (currentDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = dateFormat.parse(currentDate) ?: Date()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                binding.etDueDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateInput(): Boolean {
        val title = binding.etTitle.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.title_required)
            return false
        }

        binding.tilTitle.error = null
        return true
    }

    private fun saveChore() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val chore = Chore(
            id = if (choreId != -1) choreId else null,
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            status = binding.actvStatus.text.toString(),
            priority = binding.actvPriority.text.toString(),
            due_date = binding.etDueDate.text.toString().trim().ifEmpty { null },
            points = binding.etPoints.text.toString().toIntOrNull() ?: 0
        )

        android.util.Log.d("ChoreDetailActivity", "Saving chore - isNew: ${choreId == -1}, choreId: $choreId")
        android.util.Log.d("ChoreDetailActivity", "Chore data: ${chore.title}, ${chore.status}, ${chore.priority}")

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@ChoreDetailActivity)

                val response = if (choreId == -1) {
                    // Create new chore
                    android.util.Log.d("ChoreDetailActivity", "Creating new chore via POST")
                    apiService.createChore(chore)
                } else {
                    // Update existing chore
                    android.util.Log.d("ChoreDetailActivity", "Updating chore via PUT, id: $choreId")
                    apiService.updateChore(choreId!!, chore)
                }

                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true

                if (response.isSuccessful) {
                    val savedChore = response.body()?.chore
                    Toast.makeText(
                        this@ChoreDetailActivity,
                        "Chore saved! ID: ${savedChore?.id}",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        this@ChoreDetailActivity,
                        "Failed to save: ${response.code()} - $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true

                Toast.makeText(
                    this@ChoreDetailActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Chore")
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton("Delete") { _, _ ->
                deleteChore()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteChore() {
        if (choreId == -1) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnDelete.isEnabled = false

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@ChoreDetailActivity)
                val response = apiService.deleteChore(choreId!!)

                binding.progressBar.visibility = View.GONE
                binding.btnDelete.isEnabled = true

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ChoreDetailActivity,
                        getString(R.string.delete_success),
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this@ChoreDetailActivity,
                        "Failed to delete chore: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnDelete.isEnabled = true

                Toast.makeText(
                    this@ChoreDetailActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()
            }
        }
    }

    private fun enableEditMode() {
        isEditMode = true
        binding.etTitle.isEnabled = true
        binding.etDescription.isEnabled = true
        binding.actvStatus.isEnabled = true
        binding.actvPriority.isEnabled = true
        binding.etDueDate.isEnabled = true
        binding.etPoints.isEnabled = true
    }

    private fun disableEditMode() {
        isEditMode = false
        binding.etTitle.isEnabled = false
        binding.etDescription.isEnabled = false
        binding.actvStatus.isEnabled = false
        binding.actvPriority.isEnabled = false
        binding.etDueDate.isEnabled = false
        binding.etPoints.isEnabled = false
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}