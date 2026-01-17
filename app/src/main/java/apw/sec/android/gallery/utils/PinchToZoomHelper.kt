package apw.sec.android.gallery.utils

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PinchToZoomHelper(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val minSpanCount: Int,
    private val maxSpanCount: Int,
    private var currentSpanCount: Int,
    private val allowedSpanCounts: List<Int> = emptyList(),
    private val onSpanCountChanged: (Int) -> Unit
) {
    private val scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f
    private var isScaling = false
    private val scaleThreshold = 0.15f
    private var hasChangedInCurrentGesture = false

    private val useDiscreteSteps = allowedSpanCounts.isNotEmpty()
    private var currentStepIndex = if (useDiscreteSteps) {
        allowedSpanCounts.indexOf(currentSpanCount).coerceAtLeast(0)
    } else {
        0
    }

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

        // Set initial span count to grid
        updateGridSpanCount(currentSpanCount, animated = false)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScaling = false
                scaleFactor = 1f
            }
        }

        return isScaling
    }

    private fun updateGridSpanCount(spanCount: Int, animated: Boolean = true) {
        if (currentSpanCount == spanCount) return

        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return

        // Store scroll position to maintain it during transition
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
        val offset = firstVisibleView?.top ?: 0

        currentSpanCount = spanCount
        layoutManager.spanCount = spanCount

        if (animated) {
            recyclerView.adapter?.notifyItemRangeChanged(0, recyclerView.adapter?.itemCount ?: 0)

            // Restore scroll position after animation
            recyclerView.post {
                layoutManager.scrollToPositionWithOffset(firstVisiblePosition, offset)
            }
        } else {
            // Non-animated update (for initial setup)
            recyclerView.post {
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }

        onSpanCountChanged(spanCount)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            scaleFactor = 1f
            hasChangedInCurrentGesture = false // Reset flag at the start of each gesture
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // If we've already changed span count in this gesture, ignore further changes
            if (hasChangedInCurrentGesture) {
                return true
            }

            scaleFactor *= detector.scaleFactor

            if (useDiscreteSteps) {
                if (scaleFactor < 1f - scaleThreshold) {
                    // Pinch in - increase span count (zoom out)
                    if (currentStepIndex < allowedSpanCounts.size - 1) {
                        currentStepIndex++
                        val newSpanCount = allowedSpanCounts[currentStepIndex]
                        updateGridSpanCount(newSpanCount, animated = true)
                        hasChangedInCurrentGesture = true // Mark that we've changed
                        scaleFactor = 1f
                    }
                } else if (scaleFactor > 1f + scaleThreshold) {
                    // Pinch out - decrease span count (zoom in)
                    if (currentStepIndex > 0) {
                        currentStepIndex--
                        val newSpanCount = allowedSpanCounts[currentStepIndex]
                        updateGridSpanCount(newSpanCount, animated = true)
                        hasChangedInCurrentGesture = true // Mark that we've changed
                        scaleFactor = 1f
                    }
                }
            } else {
                // Use continuous range (original behavior)
                if (scaleFactor < 1f - scaleThreshold) {
                    // Pinch in - increase span count (zoom out)
                    val newSpanCount = (currentSpanCount + 1).coerceAtMost(maxSpanCount)
                    if (newSpanCount != currentSpanCount) {
                        updateGridSpanCount(newSpanCount, animated = true)
                        hasChangedInCurrentGesture = true // Mark that we've changed
                        scaleFactor = 1f
                    }
                } else if (scaleFactor > 1f + scaleThreshold) {
                    // Pinch out - decrease span count (zoom in)
                    val newSpanCount = (currentSpanCount - 1).coerceAtLeast(minSpanCount)
                    if (newSpanCount != currentSpanCount) {
                        updateGridSpanCount(newSpanCount, animated = true)
                        hasChangedInCurrentGesture = true // Mark that we've changed
                        scaleFactor = 1f
                    }
                }
            }

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
            scaleFactor = 1f
            hasChangedInCurrentGesture = false // Reset flag when gesture ends
        }
    }
}