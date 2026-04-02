package com.example.agreewise.ui.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.agreewise.ml.KeywordDatabase
import com.google.mlkit.vision.text.Text

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var textBlocks: List<Text.TextBlock> = emptyList()
    private var startTime: Long = 0
    private val analyzeDelay = 4000L // 4 seconds delay before showing risk colors

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun updateOverlay(blocks: List<Text.TextBlock>) {
        if (this.textBlocks.isEmpty() && blocks.isNotEmpty()) {
            startTime = System.currentTimeMillis()
        } else if (blocks.isEmpty()) {
            startTime = 0
        }
        this.textBlocks = blocks
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val currentTime = System.currentTimeMillis()
        val isAnalyzing = startTime != 0L && (currentTime - startTime) < analyzeDelay

        for (block in textBlocks) {
            for (line in block.lines) {
                val text = line.text.lowercase()
                
                // Color logic: Gray while scanning, then Red/Yellow/Green
                val color = if (isAnalyzing) {
                    Color.LTGRAY // Neutral gray during the first 4 seconds
                } else {
                    getRiskColor(text)
                }

                line.boundingBox?.let { rect ->
                    // Draw highlight fill
                    fillPaint.color = color
                    fillPaint.alpha = if (isAnalyzing) 100 else 80 
                    canvas.drawRect(rect, fillPaint)

                    // Draw border
                    borderPaint.color = color
                    borderPaint.alpha = 200
                    canvas.drawRect(rect, borderPaint)
                }
            }
        }
    }

    private fun getRiskColor(text: String): Int {
        var score = 0
        for ((keyword, weight) in KeywordDatabase.keywordWeights) {
            if (text.contains(keyword.lowercase())) {
                score += weight
            }
        }

        return when {
            score >= 40 -> Color.RED
            score >= 20 -> Color.YELLOW
            else -> Color.GREEN
        }
    }
}