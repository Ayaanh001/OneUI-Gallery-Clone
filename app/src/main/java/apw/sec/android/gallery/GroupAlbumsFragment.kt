package apw.sec.android.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.Albums
import apw.sec.android.gallery.R
import dev.oneuiproject.oneui.layout.ToolbarLayout
import apw.sec.android.gallery.AlbumRepository
import apw.sec.android.gallery.GroupAlbumAdapter
import android.view.MotionEvent
import android.content.Context
import apw.sec.android.gallery.utils.PinchToZoomHelper


class GroupAlbumsFragment : Fragment() {

    private var albums: List<AlbumItem.Album> = emptyList()
    private lateinit var pinchHelper: PinchToZoomHelper
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val PREF_GROUP_SPAN_COUNT = "group_albums_span_count"
    private val DEFAULT_SPAN_COUNT = 3

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_album, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

        val groupName = requireArguments().getString(ARG_GROUP)!!

        val group = AlbumRepository.albumItems
            .filterIsInstance<AlbumItem.Group>()
            .firstOrNull { it.groupName == groupName }

        albums = group?.albums ?: emptyList()

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_view)

        // Load saved span count
        val savedSpanCount = sharedPreferences.getInt(PREF_GROUP_SPAN_COUNT, DEFAULT_SPAN_COUNT)

        recycler.layoutManager = GridLayoutManager(context, savedSpanCount)
        recycler.adapter = GroupAlbumAdapter(requireContext(), albums)

        // Setup pinch-to-zoom
        setupPinchToZoom(recycler, savedSpanCount)

        (activity as? GroupAlbumActivity)?.let { act ->
            val toolbarLayout = act.findViewById<ToolbarLayout>(R.id.toolbar)
            val subtitle = "${albums.size} album${if (albums.size != 1) "s" else ""}"
            toolbarLayout.setTitle(groupName)
            toolbarLayout.toolbar.subtitle = subtitle
            toolbarLayout.setExpandedSubtitle(subtitle)
        }
    }

    private fun setupPinchToZoom(recyclerView: RecyclerView, savedSpanCount: Int) {
        pinchHelper = PinchToZoomHelper(
            context = requireContext(),
            recyclerView = recyclerView,
            minSpanCount = 2,
            maxSpanCount = 4,
            currentSpanCount = savedSpanCount,
            onSpanCountChanged = { newSpanCount ->
                sharedPreferences.edit()
                    .putInt(PREF_GROUP_SPAN_COUNT, newSpanCount)
                    .apply()
            }
        )

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return pinchHelper.onTouchEvent(e)
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                pinchHelper.onTouchEvent(e)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    companion object {
        private const val ARG_GROUP = "group"

        fun newInstance(groupName: String) =
            GroupAlbumsFragment().apply {
                arguments = bundleOf(ARG_GROUP to groupName)
            }
    }
}
