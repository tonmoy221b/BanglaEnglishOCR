package com.example.banglaocr

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : AppCompatActivity() {

    private lateinit var shutter: MaterialButton
    private lateinit var progress: View
    private lateinit var executor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        shutter = findViewById(R.id.btnShutter)
        progress = findViewById(R.id.progressBox)
        executor = Executors.newSingleThreadExecutor()

        startCamera()
        executor.execute {
            val ok = try { OcrEngine.init(applicationContext) } catch (e: Exception) { false }
            runOnUiThread {
                if (ok) shutter.isEnabled = true
                else Toast.makeText(this,
                    "Language data missing: add ben/eng traineddata to assets/tessdata/",
                    Toast.LENGTH_LONG).show()
            }
        }
        shutter.setOnClickListener { capture() }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            imageCapture = capture
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capture() {
        val ic = imageCapture ?: return
        shutter.isEnabled = false
        progress.visibility = View.VISIBLE
        ic.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val text = try {
                    val bmp = OcrEngine.rotate(image.toBitmap(), image.imageInfo.rotationDegrees)
                    OcrEngine.recognize(bmp)
                } catch (e: Exception) { "" } finally { image.close() }
                runOnUiThread {
                    progress.visibility = View.GONE
                    ResultDialog.show(this@CaptureActivity, text) { shutter.isEnabled = true }
                }
            }
            override fun onError(exception: ImageCaptureException) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    shutter.isEnabled = true
                    Toast.makeText(this@CaptureActivity, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
