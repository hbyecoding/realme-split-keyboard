package dev.splitkeyboard

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View

class KeyboardView(context: Context, private val changed: (ByteArray) -> Unit) : View(context) {
    private val state = KeyState()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var left = true
    private val cells = mutableListOf<Pair<RectF, Key>>()
    private val vibrator = context.getSystemService(Vibrator::class.java)
    var vibrationEnabled = true
    init { contentDescription = "多点触控分体键盘"; isFocusable = true }
    fun side(value: Boolean) { release(); left = value; arrange(); invalidate() }
    fun release() { state.clear(); changed(state.report()); invalidate() }
    private fun arrange() {
        cells.clear()
        val rows = Layouts.rows(left)
        val widthScales = listOf(1.5f, 1.5f, 1.5f, .85f)
        val offsets = if (left) listOf(-.22f, 0f, .22f, 0f) else listOf(-.22f, 0f, -.6f, 0f)
        val maxUnits = rows.take(3).mapIndexed { index, row ->
            row.sumOf { it.weight.toDouble() }.toFloat() * widthScales[index] + 2 * kotlin.math.abs(offsets[index])
        }.max()
        val availableWidth = (width - paddingLeft - paddingRight).toFloat()
        // Leave room on the left and anchor the right-hand cluster farther right.
        val unit = availableWidth / (maxUnits + if (left) 0f else 1.2f)
        val rightShift = if (left) 0f else .6f * unit
        val bottomWidth = 9f * unit
        val rowHeight = height / 4f
        var top = 0f
        val gap = 4 * resources.displayMetrics.density
        rows.forEachIndexed { rowIndex, keys ->
            val rowUnit = unit * widthScales[rowIndex]
            val rowWidth = if (rowIndex == 3) bottomWidth else rowUnit * keys.sumOf { it.weight.toDouble() }.toFloat()
            var x = paddingLeft + (availableWidth - rowWidth) / 2f + unit * offsets[rowIndex] + rightShift
            keys.forEach { key ->
                val keyWidth = if (rowIndex == 3 && key.usage == 44) bottomWidth - 3 * rowUnit else rowUnit * key.weight
                val bottom = top + rowHeight
                cells += RectF(x + gap, top + gap, x + keyWidth - gap, bottom) to key
                x += keyWidth
            }
            top += rowHeight
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { arrange() }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(13, 18, 28))
        for ((rect, key) in cells) {
            paint.color = if (state.active(key.usage)) Color.rgb(52, 153, 154) else Color.rgb(38, 49, 66)
            canvas.drawRoundRect(rect, 12f, 12f, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = minOf(rect.height() * .34f, rect.width() / maxOf(key.label.length, 2) * 1.3f)
            canvas.drawText(key.label, rect.centerX(), rect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
            if (key.usage == 13) {
                val markerWidth = rect.width() * .18f
                val markerTop = rect.bottom - rect.height() * .14f
                canvas.drawRoundRect(rect.centerX() - markerWidth / 2, markerTop,
                    rect.centerX() + markerWidth / 2, markerTop + 3 * resources.displayMetrics.density,
                    3f, 3f, paint)
            }
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                cells.firstOrNull { it.first.contains(event.getX(index), event.getY(index)) }?.let {
                    state.down(event.getPointerId(index), it.second.usage)
                    if (vibrationEnabled && vibrator?.hasVibrator() == true) {
                        val effect = if (it.second.usage == 13)
                            VibrationEffect.createWaveform(longArrayOf(0, 18, 35, 18), intArrayOf(0, 150, 0, 150), -1)
                        else VibrationEffect.createOneShot(12, 80)
                        vibrator.vibrate(effect)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> state.up(event.getPointerId(index))
            MotionEvent.ACTION_CANCEL -> state.clear()
        }
        changed(state.report()); invalidate()
        return true
    }
}
