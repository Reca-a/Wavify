package pl.edu.ur.ar131498.wavify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AuthorsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: AuthorsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tracks, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyState = view.findViewById(R.id.emptyState)
        
        setupRecyclerView()
        observeData()
        
        return view
    }

    private fun setupRecyclerView() {
        adapter = AuthorsAdapter { authorName ->
            viewModel.updateSearchQuery(authorName)
            (activity as? MainActivity)?.switchToTracksTab()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.songs.observe(viewLifecycleOwner) { songs: List<AudioFile> ->
            updateAuthors(viewModel.searchQuery.value ?: "", songs)
        }
        
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            updateAuthors(query, viewModel.songs.value ?: emptyList())
        }
        
        viewModel.sortOrder.observe(viewLifecycleOwner) {
             updateAuthors(viewModel.searchQuery.value ?: "", viewModel.songs.value ?: emptyList())
        }
    }
    
    private fun updateAuthors(query: String, allSongs: List<AudioFile>) {
        val allAuthors = allSongs.mapNotNull { it.artist }
            .filter { it != "<unknown>" }
            .distinct()
            
        // Getting sort order from ViewModel
        val sortOrder = viewModel.sortOrder.value ?: SortOrder.DATE_DESC
        
        val sortedAuthors = when(sortOrder) {
            SortOrder.ARTIST_DESC, SortOrder.TITLE_DESC, SortOrder.DATE_DESC -> allAuthors.sortedDescending()
            else -> allAuthors.sorted()
        }

        val filteredAuthors = if (query.isEmpty()) {
            sortedAuthors
        } else {
            sortedAuthors.filter { it.contains(query, ignoreCase = true) }
        }
        
        adapter.submitList(filteredAuthors)
        
        if (filteredAuthors.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyState.text = getString(R.string.no_authors)
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
}
