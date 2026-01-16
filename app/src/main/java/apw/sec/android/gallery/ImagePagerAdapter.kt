package apw.sec.android.gallery

import android.content.Context
import android.net.Uri
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.databinding.ItemImageViewerBinding
import com.bumptech.glide.Glide
import io.getstream.photoview.PhotoView

class ImagePagerAdapter(
    private val context: Context,
    private val mediaFiles: List<MediaFile>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemImageViewerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun getVideoSurfaceView(): SurfaceView? {
            return binding.videoSurface
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageViewerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaFile = mediaFiles[position]
        val uri = mediaFile.uri.toUri()
        val isVideo = isVideoFile(uri)

        if (isVideo) {
            // Show video surface, hide image view
            holder.binding.imageView.visibility = View.GONE
            holder.binding.videoSurface.visibility = View.VISIBLE

            // Remove PhotoView click listener for videos
            holder.binding.imageView.setOnPhotoTapListener(null)
            holder.binding.imageView.setOnViewTapListener(null)

            // Setup video zoom and click handlers
            setupVideoZoom(holder.binding.videoSurface)

        } else {
            // Show image view, hide video surface
            holder.binding.videoSurface.visibility = View.GONE
            holder.binding.imageView.visibility = View.VISIBLE

            // Configure PhotoView for better zoom experience
            setupPhotoView(holder.binding.imageView)

            // Load image with Glide
            Glide.with(context)
                .load(mediaFile.uri)
                .into(holder.binding.imageView)

            // Remove video surface click listener
            holder.binding.videoSurface.setOnClickListener(null)
        }
    }

    private fun setupPhotoView(photoView: PhotoView) {
        // Enable zoom
        photoView.isZoomable = true

        // Set zoom levels - IMPORTANT: Set maximum first, then medium
        photoView.maximumScale = 4f
        photoView.mediumScale = 2.5f  // This will be our target zoom level
        photoView.minimumScale = 1f

        // Enable smooth zooming
        photoView.setAllowParentInterceptOnEdge(true)

        // Set scale type
        photoView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

        // Custom double-tap behavior: zoom to 2.5x or zoom out to 1x
        photoView.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val currentScale = photoView.scale

                if (currentScale > 1.01f) {
                    // If zoomed in, zoom out to 1x
                    photoView.setScale(1f, e.x, e.y, true)
                } else {
                    // If at 1x, zoom to 2.5x at tap location
                    photoView.setScale(2.5f, e.x, e.y, true)
                }
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean = false

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Handle single tap on the PhotoView to toggle UI
                onImageClick()
                return true
            }
        })

        // Handle single tap to toggle UI - This is the primary method
        photoView.setOnPhotoTapListener { _, _, _ ->
            onImageClick()
        }

        // Handle view tap (tap outside image) to toggle UI
        photoView.setOnViewTapListener { _, _, _ ->
            onImageClick()
        }
    }

    private fun setupVideoZoom(videoSurface: SurfaceView) {
        var scale = 1f
        var isZoomed = false
        var lastTouchX = 0f
        var lastTouchY = 0f
        var activePointerId = -1

        val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    // Set pivot to the focus point of the pinch gesture
                    videoSurface.pivotX = detector.focusX
                    videoSurface.pivotY = detector.focusY
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val newScale = (scale * scaleFactor).coerceIn(1f, 4f)

                    videoSurface.scaleX = newScale
                    videoSurface.scaleY = newScale

                    scale = newScale
                    isZoomed = scale > 1.01f
                    return true
                }
            })

        val gestureDetector = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isZoomed) {
                        // Zoom out - reset to center
                        videoSurface.pivotX = videoSurface.width / 2f
                        videoSurface.pivotY = videoSurface.height / 2f
                        animateZoom(videoSurface, scale, 1f)
                        scale = 1f
                        isZoomed = false
                    } else {
                        // Zoom in to 2.5x at tap location
                        val targetScale = 2.5f

                        videoSurface.pivotX = e.x
                        videoSurface.pivotY = e.y

                        animateZoom(videoSurface, scale, targetScale)
                        scale = targetScale
                        isZoomed = true
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onImageClick()
                    return true
                }
            })

        videoSurface.setOnTouchListener { view, event ->
            // Always pass to scale detector first
            val scaleHandled = scaleGestureDetector.onTouchEvent(event)
            val gestureHandled = gestureDetector.onTouchEvent(event)

            // Handle panning when zoomed and not pinching
            if (isZoomed && !scaleGestureDetector.isInProgress) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activePointerId = event.getPointerId(0)
                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (activePointerId != -1) {
                            val pointerIndex = event.findPointerIndex(activePointerId)
                            if (pointerIndex != -1) {
                                val x = event.getX(pointerIndex)
                                val y = event.getY(pointerIndex)

                                val dx = x - lastTouchX
                                val dy = y - lastTouchY

                                videoSurface.translationX += dx
                                videoSurface.translationY += dy

                                // Constrain translation
                                val maxTranslationX = (videoSurface.width * (scale - 1)) / 2f
                                val maxTranslationY = (videoSurface.height * (scale - 1)) / 2f

                                videoSurface.translationX = videoSurface.translationX.coerceIn(-maxTranslationX, maxTranslationX)
                                videoSurface.translationY = videoSurface.translationY.coerceIn(-maxTranslationY, maxTranslationY)

                                lastTouchX = x
                                lastTouchY = y
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activePointerId = -1
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        val pointerIndex = event.actionIndex
                        val pointerId = event.getPointerId(pointerIndex)
                        if (pointerId == activePointerId) {
                            // Pick a new active pointer
                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
                            lastTouchX = event.getX(newPointerIndex)
                            lastTouchY = event.getY(newPointerIndex)
                            activePointerId = event.getPointerId(newPointerIndex)
                        }
                    }
                }
            }

            true
        }
    }

    private fun animateZoom(view: View, fromScale: Float, toScale: Float) {
        view.animate()
            .scaleX(toScale)
            .scaleY(toScale)
            .setDuration(200)
            .start()

        // Reset translation when zooming out completely
        if (toScale == 1f) {
            view.animate()
                .translationX(0f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    override fun getItemCount() = mediaFiles.size

    private fun isVideoFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }
}