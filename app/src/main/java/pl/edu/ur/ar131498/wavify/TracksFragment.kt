package pl.edu.ur.ar131498.wavify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TracksFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: AudioAdapter
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
        adapter = AudioAdapter { list, position ->
            (activity as? MainActivity)?.openAudioActivity(list, position)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            filterSongs(viewModel.searchQuery.value ?: "", songs)
        }
        
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            filterSongs(query, viewModel.songs.value ?: emptyList())
        }
    }
    
    private fun filterSongs(query: String, allSongs: List<AudioFile>) {
        val filtered = if (query.isEmpty()) {
            allSongs
        } else {
            allSongs.filter { 
                it.title.contains(query, ignoreCase = true) || 
                (it.artist?.contains(query, ignoreCase = true) == true)
            }
        }
        adapter.submitList(filtered)
    }
}
