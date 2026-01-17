package apw.sec.android.gallery.utils

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class PinchToZoomHelper(
    context: Context,
    private val recyclerView: RecyclerView,
    private val minSpanCount: Int = 2,
    private val maxSpanCount: Int = 6,
    private var currentSpanCount: Int = 3,
    private val onSpanCountChanged: ((Int) -> Unit)? = null
) {

    private var accumulatedScale = 1f
    private var isScaling = false
    private var lastSpanCount = currentSpanCount

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                accumulatedScale = 1f
                lastSpanCount = currentSpanCount
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Accumulate the scale factor for smooth, real-time updates
                accumulatedScale *= detector.scaleFactor

                // Calculate target span count based on accumulated scale
                // Using a logarithmic approach for smoother transitions
                val scaleRange = maxSpanCount - minSpanCount
                val normalizedScale = when {
                    accumulatedScale > 1f -> {
                        // Zooming in (fewer columns)
                        val zoomIn = (accumulatedScale - 1f) * 2f // Sensitivity multiplier
                        -zoomIn.coerceIn(0f, scaleRange.toFloat())
                    }
                    accumulatedScale < 1f -> {
                        // Zooming out (more columns)
                        val zoomOut = (1f - accumulatedScale) * 2f // Sensitivity multiplier
                        zoomOut.coerceIn(0f, scaleRange.toFloat())
                    }
                    else -> 0f
                }

                val targetSpanCount = (lastSpanCount + normalizedScale.roundToInt())
                    .coerceIn(minSpanCount, maxSpanCount)

                // Update in real-time for smooth feedback
                if (targetSpanCount != currentSpanCount) {
                    updateSpanCount(targetSpanCount, smooth = true)
                }

                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
                accumulatedScale = 1f
                // Final update with the current span count
                onSpanCountChanged?.invoke(currentSpanCount)
            }
        }
    )

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return isScaling
    }

    private fun updateSpanCount(newSpanCount: Int, smooth: Boolean = false) {
        if (currentSpanCount == newSpanCount) return

        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return

        // Store scroll position to maintain it during transition
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
        val offset = firstVisibleView?.top ?: 0

        currentSpanCount = newSpanCount

        // Update span count
        layoutManager.spanCount = newSpanCount

        if (smooth) {
            // Smooth, immediate update for real-time feedback
            recyclerView.adapter?.notifyItemRangeChanged(0, recyclerView.adapter?.itemCount ?: 0)

            // Restore scroll position
            recyclerView.post {
                layoutManager.scrollToPositionWithOffset(firstVisiblePosition, offset)
            }
        } else {
            // Standard update
            recyclerView.post {
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    fun getCurrentSpanCount(): Int = currentSpanCount

    fun setSpanCount(spanCount: Int) {
        updateSpanCount(spanCount.coerceIn(minSpanCount, maxSpanCount), smooth = false)
    }
}