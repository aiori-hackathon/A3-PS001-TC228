
package com.example.ransomguard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class EventChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val labels = listOf(
        "CREATE",
        "MODIFY",
        "RENAME",
        "DELETE"
    )

    private val values = mutableListOf(0, 0, 0, 0)

    private val colors = listOf(
        Color.rgb(34, 197, 94),
        Color.rgb(59, 130, 246),
        Color.rgb(245, 158, 11),
        Color.rgb(239, 68, 68)
    )

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 30f
    }

    fun updateCounts(
        create: Int,
        modify: Int,
        rename: Int,
        delete: Int
    ) {
        values[0] = create
        values[1] = modify
        values[2] = rename
        values[3] = delete

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = 150f
        val right = width - 35f
        val top = 35f
        val rowHeight = 95f

        val maxValue = maxOf(
            values.maxOrNull() ?: 0,
            1
        )

        labels.forEachIndexed { index, label ->

            val y = top + index * rowHeight

            textPaint.textSize = 27f
            textPaint.color = Color.DKGRAY

            canvas.drawText(
                label,
                10f,
                y + 30f,
                textPaint
            )

            val availableWidth = right - left

            val barWidth =
                availableWidth * values[index] / maxValue

            barPaint.color = colors[index]

            val rect = RectF(
                left,
                y,
                left + barWidth.coerceAtLeast(5f),
                y + 35f
            )

            canvas.drawRoundRect(
                rect,
                12f,
                12f,
                barPaint
            )

            textPaint.color = Color.BLACK
            textPaint.textSize = 24f

            canvas.drawText(
                values[index].toString(),
                left + barWidth + 12f,
                y + 28f,
                textPaint
            )
        }
    }
}
