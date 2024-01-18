package com.example.halalscanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import com.example.halalscanner.barcodeScan.ScanActivity
import com.example.halalscanner.history.HistoryActivity
import com.example.halalscanner.textScan.ScanTextActivity
import com.example.halalscanner.typeIngredients.TypeIngredientsActivity

// This is the main activity of the app
class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "MyPrefsFile"
    private val PREFS_DISCLAIMER_ACCEPTED = "disclaimer_accepted"

    // This function sets the click listener for a view (Button or ImageButton)
    private fun <T: View> setClickListener(viewId: Int, activity: Class<*>, viewClass: Class<T>) {
        val view = findViewById<T>(viewId)
        view.setOnClickListener {
            val intent = Intent(this, activity)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if the disclaimer has been accepted
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val disclaimerAccepted = prefs.getBoolean(PREFS_DISCLAIMER_ACCEPTED, false)

        if (!disclaimerAccepted) {
            // Show the disclaimer
            val builder = AlertDialog.Builder(this)
                .setTitle("Disclaimer")
                .setMessage("This app should only be used as a guide. " +
                    "The information provided is not guaranteed to be accurate. " +
                    "Verify the item's halal status for yourself before consumption. " +
                    "If you are unsure, contact the manufacturer.")
                .setPositiveButton("OK") { _, _ ->
                // Set the disclaimer as accepted
                val editor = prefs.edit()
                editor.putBoolean(PREFS_DISCLAIMER_ACCEPTED, true)
                editor.apply()
            }
                .setOnDismissListener() { _ ->
                // Set the disclaimer as accepted
                val editor = prefs.edit()
                editor.putBoolean(PREFS_DISCLAIMER_ACCEPTED, true)
                editor.apply()
            }
            builder.show()
        }

        // Set click listener for ScanButton to start ScanActivity
        setClickListener(R.id.ScanButton, ScanActivity::class.java, Button::class.java)

        // Set click listener for ScanTextButton to start ScanTextActivity
        setClickListener(R.id.ScanTextButton, ScanTextActivity::class.java, Button::class.java)

        // Set click listener for TypeIngredients to start TypeIngredientsActivity
        setClickListener(R.id.TypeIngredients, TypeIngredientsActivity::class.java, Button::class.java)

        // Set click listener for history to start HistoryActivity
        setClickListener(R.id.history, HistoryActivity::class.java, ImageButton::class.java)
    }
}