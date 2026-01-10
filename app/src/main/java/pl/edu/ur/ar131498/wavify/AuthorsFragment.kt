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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tracks, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        
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
        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            updateAuthors(viewModel.searchQuery.value ?: "", songs)
        }
        
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            updateAuthors(query, viewModel.songs.value ?: emptyList())
        }
    }
    
    private fun updateAuthors(query: String, allSongs: List<AudioFile>) {
        val allAuthors = allSongs.mapNotNull { it.artist }
            .filter { it != "<unknown>" }
            .distinct()
            .sorted()

        val filteredAuthors = if (query.isEmpty()) {
            allAuthors
        } else {
            allAuthors.filter { it.contains(query, ignoreCase = true) }
        }
        
        adapter.submitList(filteredAuthors)
    }
}
