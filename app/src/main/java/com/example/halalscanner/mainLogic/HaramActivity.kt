package com.example.halalscanner.mainLogic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.halalscanner.R
import androidx.appcompat.app.AppCompatActivity
import com.example.halalscanner.MainActivity

class HaramActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_haram)

        val mainMenuButton = findViewById<Button>(R.id.mainMenuButton)

        mainMenuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}