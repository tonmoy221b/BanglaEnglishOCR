package com.example.banglaocr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var pending: Class<*>? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pending?.let { startActivity(Intent(this, it)) }
            else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<android.view.View>(R.id.btnLive).setOnClickListener { open(LiveScanActivity::class.java) }
        findViewById<android.view.View>(R.id.btnCapture).setOnClickListener { open(CaptureActivity::class.java) }
    }

    private fun open(target: Class<*>) {
        pending = target
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startActivity(Intent(this, target))
        else permLauncher.launch(Manifest.permission.CAMERA)
    }
}
