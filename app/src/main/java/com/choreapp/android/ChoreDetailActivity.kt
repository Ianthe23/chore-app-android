package com.choreapp.android

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.choreapp.android.databinding.ActivityChoreDetailBinding
import com.choreapp.android.models.Chore
import com.choreapp.android.repository.ChoreRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChoreDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChoreDetailBinding
    private lateinit var repository: ChoreRepository
    private var choreId: Int? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoreDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = ChoreRepository(this)

        setupDropdowns()
        loadChoreData()
        setupListeners()
    }

    private fun setupDropdowns() {
        val statuses = arrayOf("pending", "in-progress", "completed")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actvStatus.setAdapter(statusAdapter)

        val priorities = arrayOf("low", "medium", "high")
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorities)
        binding.actvPriority.setAdapter(priorityAdapter)
    }

    private fun loadChoreData() {
        choreId = intent.getIntExtra("chore_id", -1)

        if (choreId == -1) {
            isEditMode = true
            supportActionBar?.title = "New Chore"
            enableEditMode()

            binding.actvStatus.setText("pending", false)
            binding.actvPriority.setText("medium", false)
            binding.etPoints.setText("0")

            binding.btnDelete.visibility = View.GONE
        } else {
            supportActionBar?.title = "Chore Details"
            loadExistingChore()
        }
    }

    private fun loadExistingChore() {
        binding.etTitle.setText(intent.getStringExtra("chore_title") ?: "")
        binding.etDescription.setText(intent.getStringExtra("chore_description") ?: "")
        binding.actvStatus.setText(intent.getStringExtra("chore_status") ?: "pending", false)
        binding.actvPriority.setText(intent.getStringExtra("chore_priority") ?: "medium", false)
        binding.etDueDate.setText(intent.getStringExtra("chore_due_date") ?: "")
        binding.etPoints.setText(intent.getIntExtra("chore_points", 0).toString())

        enableEditMode()
    }

    private fun setupListeners() {
        binding.etDueDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveChore()
            }
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etDueDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun validateInput(): Boolean {
        val title = binding.etTitle.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return false
        }

        binding.tilTitle.error = null
        return true
    }

    private fun saveChore() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val status = binding.actvStatus.text.toString()
        val priority = binding.actvPriority.text.toString()
        val dueDate = binding.etDueDate.text.toString().ifEmpty { null }
        val points = binding.etPoints.text.toString().toIntOrNull() ?: 0

        val chore = Chore(
            id = choreId,
            title = title,
            description = description.ifEmpty { null },
            status = status,
            priority = priority,
            due_date = dueDate,
            points = points
        )

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                if (choreId == null || choreId == -1) {
                    // Create new chore (works offline with repository)
                    repository.createChore(chore)
                    Toast.makeText(
                        this@ChoreDetailActivity,
                        "Chore created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Update existing chore (works offline with repository)
                    repository.updateChore(choreId!!, chore)
                    Toast.makeText(
                        this@ChoreDetailActivity,
                        "Chore updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                finish()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE

                Toast.makeText(
                    this@ChoreDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Chore")
            .setMessage("Are you sure you want to delete this chore?")
            .setPositiveButton("Delete") { _, _ ->
                deleteChore()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteChore() {
        if (choreId == null || choreId == -1) return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Delete chore (works offline with repository)
                repository.deleteChore(choreId!!)

                Toast.makeText(
                    this@ChoreDetailActivity,
                    "Chore deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE

                Toast.makeText(
                    this@ChoreDetailActivity,
                    "Error deleting chore: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun enableEditMode() {
        binding.etTitle.isEnabled = true
        binding.etDescription.isEnabled = true
        binding.actvStatus.isEnabled = true
        binding.actvPriority.isEnabled = true
        binding.etDueDate.isEnabled = true
        binding.etPoints.isEnabled = true
        binding.btnSave.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}