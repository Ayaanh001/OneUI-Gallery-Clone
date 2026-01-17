package apw.sec.android.gallery.components

import android.widget.ImageView
import android.content.Context
import android.util.AttributeSet
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Canvas
import apw.sec.android.gallery.R

class Image @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ImageView(context, attrs, defStyleAttr){
    private var cornerRadius: Float = 0f
    private var forceSquare: Boolean = false  // force square aspect ratio
    private val path = Path()

    init{
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.Image, 0, 0)
            cornerRadius = typedArray.getDimension(R.styleable.Image_cornerRadius, cornerRadius)
            forceSquare = typedArray.getBoolean(R.styleable.Image_forceSquare, false)
            typedArray.recycle()
        }
    }

    fun setCornerRadius(cornerRadius: Float){
        this.cornerRadius = cornerRadius
        invalidate()
    }

    fun setForceSquare(forceSquare: Boolean) {
        this.forceSquare = forceSquare
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (forceSquare) {
            // Force square aspect ratio by making height equal to width
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        } else {
            // Use normal measurement
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDraw(canvas: Canvas){
        if (cornerRadius > 0f) {
            path.reset()
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(path)
        }
        super.onDraw(canvas)
    }
}