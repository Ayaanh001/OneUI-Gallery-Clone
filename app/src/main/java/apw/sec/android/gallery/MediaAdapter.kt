package apw.sec.android.gallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import apw.sec.android.gallery.components.Image
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import apw.sec.android.gallery.data.MediaHub
import java.util.UUID

class MediaAdapter(
    private val mediaFiles: MutableList<MediaFile>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private var currentSpanCount: Int = 4 // Default span count

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: Image = view.findViewById(R.id.imageView)
        val videoDurationOverlay: LinearLayout = view.findViewById(R.id.videoDurationOverlay)
        val videoDurationText: TextView = view.findViewById(R.id.videoDuration)

        init {
            view.setOnClickListener {
                val key: String = UUID.randomUUID().toString()
                MediaHub.save(key, mediaFiles)
                val context = itemView.context
                val intent = Intent(context, ViewActivity::class.java).apply {
                    putExtra("media_key", key)
                    putExtra("position", bindingAdapterPosition)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_item, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaFile = mediaFiles[position]

        if (mediaFile.isVideo()) {
            // Show duration overlay only if span count is 6 or less
            if (currentSpanCount <= 6) {
                holder.videoDurationOverlay.visibility = View.VISIBLE

                // Format and set duration
                val duration = mediaFile.duration ?: 0L
                holder.videoDurationText.text = formatDuration(duration)
            } else {
                // Hide duration for span count > 6 (i.e., span count 9)
                holder.videoDurationOverlay.visibility = View.GONE
            }
        } else {
            // Hide for images
            holder.videoDurationOverlay.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(mediaFile.uri)
            .centerCrop()
            .transform(RoundedCorners(20))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = mediaFiles.size

    // Method to update span count and refresh items
    fun updateSpanCount(spanCount: Int) {
        if (currentSpanCount != spanCount) {
            currentSpanCount = spanCount
            notifyItemRangeChanged(0, itemCount)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
        } else {
            String.format("%d:%02d", minutes, remainingSeconds)
        }
    }
}