package pl.edu.ur.ar131498.wavify.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import pl.edu.ur.ar131498.wavify.ui.viewmodel.MainViewModel
import pl.edu.ur.ar131498.wavify.R
import pl.edu.ur.ar131498.wavify.data.AudioFile
import pl.edu.ur.ar131498.wavify.ui.activities.MainActivity
import pl.edu.ur.ar131498.wavify.ui.adapters.AudioAdapter
import pl.edu.ur.ar131498.wavify.ui.bottomsheets.SongMenuBottomSheet

class FavoritesFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: AudioAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView

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
        adapter = AudioAdapter(
            onItemClick = { list, position ->
                (activity as? MainActivity)?.openAudioActivity(list, position)
            },
            onItemLongClick = { song ->
                SongMenuBottomSheet(requireContext(), song) {
                    viewModel.refreshFavorites()
                }.show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.favorites.observe(viewLifecycleOwner) { favs ->
            filterFavorites(viewModel.searchQuery.value ?: "", favs)
        }
        
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            filterFavorites(query, viewModel.favorites.value ?: emptyList())
        }

        viewModel.refreshFavorites()
    }
    
    private fun filterFavorites(query: String, allFavs: List<AudioFile>) {
         val filtered = if (query.isEmpty()) {
            allFavs
        } else {
            allFavs.filter { 
                it.title.contains(query, ignoreCase = true) || 
                (it.artist?.contains(query, ignoreCase = true) == true)
            }
        }
        adapter.submitList(filtered)
        
        if (filtered.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            emptyState.text = getString(R.string.no_favorites)
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshFavorites()
    }
}
