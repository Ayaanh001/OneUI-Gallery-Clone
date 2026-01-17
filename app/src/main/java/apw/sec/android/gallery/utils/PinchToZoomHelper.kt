package apw.sec.android.gallery.utils

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PinchToZoomHelper(
    context: Context,
    private val recyclerView: RecyclerView,
    private val minSpanCount: Int = 2,
    private val maxSpanCount: Int = 6,
    private var currentSpanCount: Int = 3,
    private val onSpanCountChanged: ((Int) -> Unit)? = null
) {

    private var scaleFactor = 1f
    private var isScaling = false

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor

                // Calculate new span count based on scale
                // Pinch out (zoom in) = fewer columns
                // Pinch in (zoom out) = more columns
                val targetSpanCount = when {
                    scaleFactor > 1.15f -> {
                        scaleFactor = 1f
                        (currentSpanCount - 1).coerceIn(minSpanCount, maxSpanCount)
                    }
                    scaleFactor < 0.85f -> {
                        scaleFactor = 1f
                        (currentSpanCount + 1).coerceIn(minSpanCount, maxSpanCount)
                    }
                    else -> currentSpanCount
                }

                if (targetSpanCount != currentSpanCount) {
                    updateSpanCount(targetSpanCount)
                }

                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
                scaleFactor = 1f
            }
        }
    )

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return isScaling
    }

    private fun updateSpanCount(newSpanCount: Int) {
        if (currentSpanCount == newSpanCount) return

        currentSpanCount = newSpanCount

        // Update RecyclerView's GridLayoutManager
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
        layoutManager?.spanCount = newSpanCount

        // Notify callback
        onSpanCountChanged?.invoke(newSpanCount)

        // Smooth update
        recyclerView.post {
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    fun getCurrentSpanCount(): Int = currentSpanCount

    fun setSpanCount(spanCount: Int) {
        updateSpanCount(spanCount.coerceIn(minSpanCount, maxSpanCount))
    }
}