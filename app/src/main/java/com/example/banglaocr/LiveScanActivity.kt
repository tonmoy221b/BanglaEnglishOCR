package com.example.banglaocr

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveScanActivity : AppCompatActivity() {

    private lateinit var liveText: TextView
    private lateinit var executor: ExecutorService
    @Volatile private var latest = ""
    @Volatile private var paused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        liveText = findViewById(R.id.liveText)
        executor = Executors.newSingleThreadExecutor()

        findViewById<android.view.View>(R.id.btnShow).setOnClickListener {
            paused = true
            ResultDialog.show(this, latest) { paused = false }
        }

        executor.execute {
            val ok = try { OcrEngine.init(applicationContext) } catch (e: Exception) { false }
            runOnUiThread {
                if (ok) { liveText.text = "Point camera at text…"; startCamera() }
                else {
                    liveText.text = "OCR init failed. Put ben.traineddata & eng.traineddata in assets/tessdata/"
                    Toast.makeText(this, "Language data missing", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                try {
                    if (!paused) {
                        val bmp = OcrEngine.rotate(proxy.toBitmap(), proxy.imageInfo.rotationDegrees)
                        val text = OcrEngine.recognize(bmp)
                        if (text.isNotEmpty()) {
                            latest = text
                            runOnUiThread { liveText.text = text }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    proxy.close()
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
