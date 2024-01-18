package com.example.halalscanner.typeIngredients

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.halalscanner.R
import com.example.halalscanner.mainLogic.MainLogic

// This activity is used to type ingredients manually
class TypeIngredientsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_input_ingredients) // Replace with your actual layout resource

        // Initialize the EditText and Button
        val editText = findViewById<EditText>(R.id.ingredientInput)
        val confirmButton = findViewById<Button>(R.id.submitButton)

        // Get the text passed from the previous activity
        val text = intent.getStringExtra("EXTRA_TEXT")

        if (text != null) {
            // If text is passed, set it as the text of the EditText
            editText.setText(text)
        } else {
            // If no text is passed, set a hint for the EditText
            editText.hint = "Type the ingredients here"
        }

        confirmButton.setOnClickListener {
            // Get the text from the EditText
            val inputText = editText.text.toString()

            // Check if the input is empty
            if (inputText.trim().isEmpty()) {
                // Show a Toast message if the input is empty
                Toast.makeText(this@TypeIngredientsActivity, "Please enter the ingredients.", Toast.LENGTH_SHORT).show()
            } else {
                // If the input is not empty, split it into ingredients
                val inputIngredients = inputText.split(",").map { it.trim() }

                // Create an instance of MainLogic and check if the ingredients are halal
                val mainLogic = MainLogic(this@TypeIngredientsActivity)
                mainLogic.isHalal(this@TypeIngredientsActivity, inputIngredients)
            }
        }
    }
}
