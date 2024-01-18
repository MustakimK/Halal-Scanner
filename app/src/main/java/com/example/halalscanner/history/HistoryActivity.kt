package com.example.halalscanner.history

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.halalscanner.MainActivity
import com.example.halalscanner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private val job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_history)

        val historyView = findViewById<RecyclerView>(R.id.historyRecyclerView)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        historyView.layoutManager = LinearLayoutManager(this)

        // Add a divider under each item
        val dividerItemDecoration = DividerItemDecoration(historyView.context, DividerItemDecoration.VERTICAL)
        historyView.addItemDecoration(dividerItemDecoration)


        val db = DatabaseManager.getInstance(this@HistoryActivity).database

        // Use coroutines to move the database operation off of the main thread
        val scope = CoroutineScope(Dispatchers.IO + job)

        scope.launch {
            val historyData = (db.historyDao().getAll()).reversed()

            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                historyView.adapter = HistoryAdapter(historyData)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

