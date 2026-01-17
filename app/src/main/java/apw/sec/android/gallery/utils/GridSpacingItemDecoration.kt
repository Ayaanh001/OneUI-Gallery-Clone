package apw.sec.android.gallery.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private var spanCount: Int,
    private val baseSpacing: Int, // Base spacing for normal span counts
    private val includeEdge: Boolean = true
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position < 0) return

        val column = position % spanCount

        // Adaptive spacing: reduce spacing as span count increases
        val spacing = when {
            spanCount <= 3 -> baseSpacing
            spanCount == 4 -> (baseSpacing * 0.75f).toInt()
            spanCount == 5 -> (baseSpacing * 0.5f).toInt()
            else -> (baseSpacing * 0.35f).toInt() // spanCount >= 6
        }

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }

    // Method to update span count dynamically
    fun updateSpanCount(newSpanCount: Int) {
        spanCount = newSpanCount
    }
}