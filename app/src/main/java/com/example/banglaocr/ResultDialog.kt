package com.example.banglaocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object ResultDialog {
    fun show(ctx: Context, text: String, onClose: (() -> Unit)? = null) {
        val shown = text.ifBlank { "No text found." }
        val pad = (20 * ctx.resources.displayMetrics.density).toInt()
        val tv = TextView(ctx).apply {
            this.text = shown
            textSize = 18f
            setTextIsSelectable(true)
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(ctx).apply { addView(tv) }

        AlertDialog.Builder(ctx)
            .setTitle("Scanned Text")
            .setView(scroll)
            .setPositiveButton("Copy") { _, _ ->
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("ocr", shown))
                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .setOnDismissListener { onClose?.invoke() }
            .show()
    }
}
