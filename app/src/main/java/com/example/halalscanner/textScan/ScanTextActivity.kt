package com.example.halalscanner.textScan

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.halalscanner.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.example.halalscanner.barcodeScan.ScanActivity
import com.example.halalscanner.mainLogic.MainLogic
import com.example.halalscanner.typeIngredients.TypeIngredientsActivity
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// This activity is used to scan text from an image
class ScanTextActivity : AppCompatActivity() {

    // Constants
    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val TAG = "ScanTextActivity"
    }

    // Executor service to run the camera operations in the background
    private lateinit var cameraExecutor: ExecutorService

    // UI elements
    private val confirmButton by lazy { findViewById<Button>(R.id.confirmButton) }
    private val viewFinder by lazy { findViewById<PreviewView>(R.id.cameraPreview) }
    private val loadingSpinner by lazy { findViewById<View>(R.id.loadingBar) }
    private val instructionText by lazy { findViewById<TextView>(R.id.Instruction) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_scanner)

        // Initialize the executor service
        cameraExecutor = Executors.newSingleThreadExecutor()

        //Set text
        instructionText.text = "Scan the ingredients list and press the button to confirm."

        // Check if the camera permission is granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            // If not, request the camera permission
            ActivityCompat.requestPermissions(this@ScanTextActivity, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                // If the camera permissions have been granted, start the camera
                startCamera()
            } else {
                Toast.makeText(this@ScanTextActivity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // This function checks if all the required permissions are granted
    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // This function starts the camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@ScanTextActivity)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Build the preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            // Build the image capture
            val imageCapture = ImageCapture.Builder()
                .build()

            // Select the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all bound camera resources
                cameraProvider.unbindAll()

                // Bind the camera provider to the lifecycle of this activity
                cameraProvider.bindToLifecycle(this@ScanTextActivity, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Log the error
                Log.e(TAG, "Use case binding failed", exc)
            }

            // Set the click listener for the confirm button
            confirmButton.setOnClickListener {
                //Unbind preview
                cameraProvider.unbind(preview)

                //Disable button
                confirmButton.isEnabled = false

                // Show the loading spinner
                loadingSpinner.visibility = View.VISIBLE

                // Take a picture and analyze it
                imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                    @OptIn(ExperimentalGetImage::class)
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // Analyze the image
                        TextAnalyzer { text ->
                            runOnUiThread {
                                // Hide the loading spinner
                                loadingSpinner.visibility = View.GONE

                                // If no ingredients are found
                                if (text == "No ingredients found") {
                                    // Show an alert dialog
                                    AlertDialog.Builder(this@ScanTextActivity)
                                        .setTitle("No Ingredients Detected")
                                        .setMessage("We couldn't find any ingredients in the image. Please try again.")
                                        .setPositiveButton("OK") { _, _ ->
                                            // Start the camera
                                            startCamera()

                                            // Enable button
                                            confirmButton.isEnabled = true
                                        }
                                        .setOnDismissListener() {
                                            // Start the camera
                                            startCamera()

                                            // Enable button
                                            confirmButton.isEnabled = true
                                        }
                                        .show()
                                } else {
                                    // If ingredients are found, show an alert dialog
                                    AlertDialog.Builder(this@ScanTextActivity)
                                        .setTitle("Text Detected")
                                        .setMessage("Is this the correct text: $text?\n")
                                        .setPositiveButton("Yes") { _, _ ->
                                            // If the user confirms the text, check if it's halal
                                            val mainLogic = MainLogic(this@ScanTextActivity)
                                            mainLogic.isHalal(this@ScanTextActivity, text.split(",", "/").map { it.trim() })
                                        }
                                        .setNegativeButton("Retry") { _, _ ->
                                            // If the user wants to retry, just dismiss the alert
                                            startCamera()

                                            // Enable button
                                            confirmButton.isEnabled = true
                                        }
                                        .setNeutralButton("Edit Text") { _, _ ->
                                            // If the user wants to edit the text, start the TypeIngredientsActivity
                                            val intent = Intent(this@ScanTextActivity, TypeIngredientsActivity::class.java).apply {
                                                putExtra("EXTRA_TEXT", text)
                                            }
                                            startActivity(intent)
                                        }
                                        .setOnDismissListener {
                                            // If the user dismisses the alert, start the camera
                                            startCamera()

                                            // Enable button
                                            confirmButton.isEnabled = true
                                        }
                                        .show()
                                }
                            }
                        }.analyze(image) // Pass the ImageProxy object here
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Log the error
                        Log.i("ScanText", "onError: $exception")
                    }
                })
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // This class is used to analyze the image
    private class TextAnalyzer(private val onTextDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        var ingredientsFound = false
                        for (block in visionText.textBlocks) {
                            val blockText = block.text
                            val index = blockText.indexOf("ingredients", ignoreCase = true)
                            if (index != -1) {
                                val ingredients = blockText.substring(index + "ingredients:".length).trim()

                                onTextDetected(ingredients)
                                ingredientsFound = true
                                break
                            }
                        }
                        if (!ingredientsFound) {
                            onTextDetected("No ingredients found")
                        }
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Start the camera
        startCamera()

        // Enable button
        confirmButton.isEnabled = true
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
