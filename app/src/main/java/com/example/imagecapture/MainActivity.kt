package com.example.imagecapture

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private val imagePaths: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
        findViewById<Button>(R.id.button).setOnClickListener {
            takePhoto()
        }
        // Populate imagePaths list on app startup
        imagePaths.addAll(getSavedImagePaths())
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            } catch (exc: Exception) {
                // Handle exception
                Log.e("Camera", "Use case binding failed", exc)
                Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Image saved successfully
                    Toast.makeText(this@MainActivity, "Image saved successfully", Toast.LENGTH_SHORT).show()
                    saveImageToInternalStorage(outputFileResults.savedUri!!)
                   // saveImagePath(savedImagePath)
                }
                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                    Toast.makeText(this@MainActivity, " Handle error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    private fun saveImageToInternalStorage(savedUri: Uri): String {
        val inputStream = contentResolver.openInputStream(savedUri)

        // Create the directory if it doesn't exist
        val directory = File(filesDir, "images")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val outputPath = File(directory, "${System.currentTimeMillis()}.jpg")

        val outputStream = FileOutputStream(outputPath)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        return outputPath.absolutePath
    }
    private fun getSavedImagePaths(): List<String> {
        val directory = File(filesDir, "images")
        return directory.listFiles()?.map { it.absolutePath } ?: emptyList()
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}