package apw.sec.android.gallery

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import apw.sec.android.gallery.databinding.*
import androidx.recyclerview.widget.*
import android.util.Log
import apw.sec.android.gallery.data.MediaHub
import apw.sec.android.gallery.utils.PinchToZoomHelper

class AlbumViewer: AppCompatActivity(){

    private var _binding: LayoutAlbumViewerBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaFiles: MutableList<MediaFile>
    private lateinit var adapter: MediaAdapter
    private lateinit var pinchHelper: PinchToZoomHelper
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val PREF_ALBUM_DETAIL_SPAN_COUNT = "album_detail_span_count"
    private val DEFAULT_SPAN_COUNT = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = LayoutAlbumViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationButtonAsBack()

        val mediaKey = intent.getStringExtra("media_key")
        val folderName = intent.getStringExtra("folderName") ?: "Album"

        binding.toolbar.setTitle(folderName)

        mediaFiles = if (mediaKey != null) {
            MediaHub.get(mediaKey)?.toMutableList() ?: mutableListOf()
        } else {
            fetchMediaFilesFromFolder(folderName).toMutableList()
        }
        mediaFiles.sortByDescending { it.dateAdded ?: 0L }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

        setupRecyclerView(mediaFiles, folderName)

        // Calculate and display counts
        val videoCount = mediaFiles.count { it.isVideo() }
        val imageCount = mediaFiles.size - videoCount

        val parts = mutableListOf<String>()
        if (imageCount > 0) parts.add("$imageCount image${if (imageCount > 1) "s" else ""}")
        if (videoCount > 0) parts.add("$videoCount video${if (videoCount > 1) "s" else ""}")
        val mediaCountText = parts.joinToString(" ")

        binding.toolbar.toolbar.setSubtitle(mediaCountText)
        binding.toolbar.setExpandedSubtitle(mediaCountText)
    }

    fun setupRecyclerView(mediaFiles: MutableList<MediaFile>, folderName: String?){
        getSupportActionBar()?.title = folderName
        Log.e("AlbumError",mediaFiles.size.toString())
        adapter = MediaAdapter(mediaFiles)

        // Load saved span count
        val savedSpanCount = sharedPreferences.getInt(PREF_ALBUM_DETAIL_SPAN_COUNT, DEFAULT_SPAN_COUNT)

        binding.recyclerView.layoutManager = GridLayoutManager(this, savedSpanCount)
        binding.recyclerView.adapter = adapter
        setupPinchToZoom(savedSpanCount)
    }

    private fun setupPinchToZoom(savedSpanCount: Int) {
        val allowedSpanCounts = listOf(2, 3, 4, 5, 6, 9)

        val initialSpanCount = allowedSpanCounts.minByOrNull {
            kotlin.math.abs(it - savedSpanCount)
        } ?: 4

        // Update adapter with initial span count
        adapter.updateSpanCount(initialSpanCount)

        pinchHelper = PinchToZoomHelper(
            context = this,
            recyclerView = binding.recyclerView,
            minSpanCount = allowedSpanCounts.first(),
            maxSpanCount = allowedSpanCounts.last(),
            currentSpanCount = initialSpanCount,
            allowedSpanCounts = allowedSpanCounts,
            onSpanCountChanged = { newSpanCount ->
                // Update adapter to show/hide video durations
                adapter.updateSpanCount(newSpanCount)

                // Save the new span count
                sharedPreferences.edit()
                    .putInt(PREF_ALBUM_DETAIL_SPAN_COUNT, newSpanCount)
                    .apply()
            }
        )

        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return pinchHelper.onTouchEvent(e)
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                pinchHelper.onTouchEvent(e)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun fetchMediaFilesFromFolder(folderName: String?): List<MediaFile> {
        val allMediaFiles = Albums(this).fetchAlbums()
        val filteredMediaFiles = allMediaFiles.filter { it.folderName == folderName }
        return filteredMediaFiles
    }

    override fun onDestroy(){
        super.onDestroy()
        _binding = null
    }
}