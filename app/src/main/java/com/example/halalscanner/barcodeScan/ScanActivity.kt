package com.example.halalscanner.barcodeScan

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


import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.halalscanner.MainActivity
import com.example.halalscanner.mainLogic.MainLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// This class is used to scan barcodes
class ScanActivity : AppCompatActivity() {

    // Constants
    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val TAG = "ScanActivity"
    }

    // Executor service for camera operations
    private lateinit var cameraExecutor: ExecutorService

    // Lazy initialization of views
    private val confirmButton by lazy { findViewById<Button>(R.id.confirmButton) }
    private val loadingSpinner by lazy { findViewById<View>(R.id.loadingBar) }
    private val viewFinder by lazy { findViewById<PreviewView>(R.id.cameraPreview) }
    private val instructionText by lazy { findViewById<TextView>(R.id.Instruction) }

    // Flag to check if scanning is active
    private var isScanningActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_scanner)

        // Hide the confirm button as it's not needed in this activity
        confirmButton.visibility = View.INVISIBLE

        instructionText.text = "Scan the barcode of the product"

        // Initialize the executor service
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check for camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            // Request for camera permissions if not granted
            ActivityCompat.requestPermissions(this@ScanActivity, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
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
                Toast.makeText(this@ScanActivity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Function to check if all permissions are granted
    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // This function is used to start the camera
    private fun startCamera() {
        // Get an instance of ProcessCameraProvider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            //Var for alert dialogs
            var alertFlag = false

            // Get the camera provider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Build the preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            // Build the image analysis
            val imageAnalysis = ImageAnalysis.Builder().build()

            // Set the analyzer for the image analysis
            imageAnalysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                runOnUiThread {
                    // If scanning is active
                    if (isScanningActive) {
                        // Pause scanning when a barcode is detected
                        cameraProvider.unbind(preview)
                        isScanningActive = false

                        // Create an instance of MainLogic
                        val mainLogic = MainLogic(this@ScanActivity)

                        CoroutineScope(Dispatchers.Main).launch {
                            // Get the product name
                            val productName = mainLogic.getProductName(barcode)

                            // Hide the loading spinner
                            loadingSpinner.visibility = View.GONE

                            // If the product name is empty
                            when (productName) {
                                "No internet connection" -> {
                                    // Handle the case where there's no internet connection
                                    AlertDialog.Builder(this@ScanActivity)
                                        .setTitle("No internet connection")
                                        .setMessage("Please check your internet connection and try again.")
                                        .setPositiveButton("Retry") { _, _ ->
                                            // Resume scanning when the user wants to retry
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .setOnDismissListener {
                                            // Resume scanning when the dialog is dismissed
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .show()
                                }

                                "API is down" -> {
                                    // Handle the case where the API is down
                                    AlertDialog.Builder(this@ScanActivity)
                                        .setTitle("Service Unavailable")
                                        .setMessage("The product information service is currently unavailable. Please try again later.")
                                        .setPositiveButton("Retry") { _, _ ->
                                            // Resume scanning when the user wants to retry
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .setOnDismissListener {
                                            // Resume scanning when the dialog is dismissed
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .show()
                                }

                                "Failed to get product name" -> {
                                    // Handle the case where there was an error getting the product name
                                    AlertDialog.Builder(this@ScanActivity)
                                        .setTitle("Error")
                                        .setMessage("An error occurred while trying to get the product name. Please try again.")
                                        .setPositiveButton("Retry") { _, _ ->
                                            // Resume scanning when the user wants to retry
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .setOnDismissListener {
                                            // Resume scanning when the dialog is dismissed
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .show()
                                }

                                "Product not found" -> {
                                    // Handle the case where the product name is empty
                                    AlertDialog.Builder(this@ScanActivity)
                                        .setTitle("No product was found")
                                        .setPositiveButton("Retry") { _, _ ->
                                            // Resume scanning when the user wants to retry
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .setNeutralButton("Go Back") { _, _ ->
                                            // Handle the user wanting to go back to the main activity
                                            val intent = Intent(
                                                this@ScanActivity,
                                                MainActivity::class.java
                                            )
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                        .setOnDismissListener {
                                            // Resume scanning when the dialog is dismissed
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .show()
                                }

                                "" -> {
                                    // Handle the case where the product name is empty
                                    AlertDialog.Builder(this@ScanActivity)
                                        .setTitle("No product name was found")
                                        .setPositiveButton("Retry") { _, _ ->
                                            // Resume scanning when the user wants to retry
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .setNeutralButton("Go Back") { _, _ ->
                                            // Handle the user wanting to go back to the main activity
                                            val intent = Intent(
                                                this@ScanActivity,
                                                MainActivity::class.java
                                            )
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                        .setOnDismissListener {
                                            // Resume scanning when the dialog is dismissed
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .show()
                                }

                                else -> {
                                    // Show an alert dialog indicating that a product was detected
                                    AlertDialog.Builder(this@ScanActivity)
                                        .setTitle("Product Detected")
                                        .setMessage("Is this the correct product: $productName?")
                                        .setPositiveButton("Yes") { _, _ ->
                                            alertFlag = true
                                            CoroutineScope(Dispatchers.Main).launch {
                                                // Get the product ingredients
                                                val ingredients = mainLogic.getProductIngredients(barcode)

                                                // If the ingredients are not null and the first ingredient is not an error message
                                                if (ingredients != null && !ingredients[0].startsWith(
                                                        "No internet connection"
                                                    ) && !ingredients[0].startsWith("API is down") && !ingredients[0].startsWith(
                                                        "Failed to get ingredients"
                                                    ) && !ingredients[0].startsWith("Ingredients not found")
                                                ) {
                                                    // Check if the product is halal
                                                    mainLogic.isHalal(
                                                        this@ScanActivity,
                                                        ingredients,
                                                        barcode,
                                                        productName
                                                    )
                                                }
                                                else {
                                                    // Show an alert dialog indicating the error message
                                                    AlertDialog.Builder(this@ScanActivity)
                                                        .setTitle("Error")
                                                        .setMessage(
                                                            ingredients?.get(0) ?: "Unknown error"
                                                        )
                                                        .setPositiveButton("Retry") { _, _ ->
                                                            startCamera()
                                                            isScanningActive = true
                                                        }
                                                        .setNegativeButton("Go Back") { _, _ ->
                                                            // Handle the user closing the error dialog
                                                            val intent = Intent(
                                                                this@ScanActivity,
                                                                MainActivity::class.java
                                                            )
                                                            intent.flags =
                                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                            startActivity(intent)
                                                        }
                                                        .setOnDismissListener {
                                                            // Resume scanning when the dialog is dismissed
                                                            startCamera()
                                                            isScanningActive = true
                                                        }
                                                        .show()
                                                }
                                            }
                                        }
                                        .setNegativeButton("Retry") { _, _ ->
                                            // Resume scanning when the user wants to retry
                                            startCamera()
                                            isScanningActive = true
                                        }
                                        .setOnDismissListener {
                                            // Resume scanning when the dialog is dismissed

                                            if (!alertFlag) {
                                                startCamera()
                                                isScanningActive = true
                                            }
                                        }
                                        .show()
                                }
                            }
                        }
                    }
                }
            })

            // Select the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all bound camera resources
                cameraProvider.unbindAll()

                // Bind the camera provider to the lifecycle of this activity
                cameraProvider.bindToLifecycle(
                    this@ScanActivity,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                // Log the error
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this@ScanActivity))
    }

    // This class is used to analyze the image
    private class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        // Get an instance of BarcodeScanning
        private val scanner = BarcodeScanning.getClient()

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            // Get the media image
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                // Create an InputImage from the media image
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                // Process the image
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        // For each barcode in the barcodes
                        for (barcode in barcodes) {
                            // Call the onBarcodeDetected function
                            onBarcodeDetected(barcode.rawValue ?: "")
                            break
                        }
                    }
                    .addOnCompleteListener {
                        // Close the image proxy
                        imageProxy.close()
                    }
            }
        }
    }

    // This function is called when the activity is resumed
    override fun onResume() {
        super.onResume()
        // Check if scanning is not active
        if (!isScanningActive) {
            // Start the camera
            startCamera()
            // Set scanning to active
            isScanningActive = true
        }
    }

    // This function is called when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
