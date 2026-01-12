package pl.edu.ur.ar131498.wavify

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import pl.edu.ur.ar131498.wavify.databinding.FragmentPlaylistsBinding

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private lateinit var playlistManager: PlaylistManager
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val viewModel: MainViewModel by activityViewModels()

    private var allPlaylists: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistManager = PlaylistManager(requireContext())

        adapter = PlaylistAdapter(
            emptyList(),
            onPlaylistClick = { name ->
                openPlaylist(name)
            },
            onDeleteClick = { name ->
                confirmDeletePlaylist(name)
            }
        )

        binding.playlistsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.playlistsRecyclerView.adapter = adapter

        binding.addPlaylistFab.setOnClickListener {
            showCreatePlaylistDialog()
        }
        
        allPlaylists = playlistManager.getPlaylists().toList()

        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            refreshList(query, viewModel.sortOrder.value)
        }
        
        viewModel.sortOrder.observe(viewLifecycleOwner) { order ->
             refreshList(viewModel.searchQuery.value ?: "", order)
        }
    }

    override fun onResume() {
        super.onResume()
        allPlaylists = playlistManager.getPlaylists().toList()
        refreshList(viewModel.searchQuery.value ?: "", viewModel.sortOrder.value)
    }

    private fun refreshPlaylists() {
         allPlaylists = playlistManager.getPlaylists().toList()
         refreshList(viewModel.searchQuery.value ?: "", viewModel.sortOrder.value)
    }

    private fun refreshList(query: String, sortOrder: SortOrder?) {
        var filtered = if (query.isEmpty()) {
            allPlaylists
        } else {
            allPlaylists.filter { it.contains(query, ignoreCase = true) }
        }

        filtered = when(sortOrder) {
            SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.lowercase() }
            else -> filtered.sortedBy { it.lowercase() }
        }

        val allSongs = MusicRepository.getLocalAudioFiles(requireContext())
        val songMap = allSongs.associateBy { it.uri.toString() }

        val uiModels = filtered.map { name ->
            val songUris = playlistManager.getSongUris(name)
            val songs = songUris.mapNotNull { songMap[it] }
            val coverUris = songs.mapNotNull { it.albumArtUri }.take(4)
            
            PlaylistUIModel(
                name = name,
                songCount = songUris.size,
                coverUris = coverUris
            )
        }
        
        adapter.updateData(uiModels)

        if (uiModels.isEmpty()){
            binding.playlistsRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyState.text = getString(R.string.empty_playlist)
        } else {
            binding.playlistsRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(requireContext())
        input.setHint(R.string.enter_playlist_name)

        input.filters = arrayOf(android.text.InputFilter.LengthFilter(20))

        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding / 2, padding, padding / 2)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()

                val regex = Regex("^[a-zA-Z0-9 _-]*$")
                
                if (name.isNotEmpty()) {
                    if (!name.matches(regex)) {
                        Toast.makeText(context, R.string.name_not_allowed, Toast.LENGTH_SHORT).show()
                    } else if (playlistManager.createPlaylist(name)) {
                        Toast.makeText(context, R.string.playlist_created, Toast.LENGTH_SHORT).show()
                        refreshPlaylists()
                    } else {
                        Toast.makeText(context, R.string.playlist_exists, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, R.string.enter_playlist_name, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeletePlaylist(name: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_playlist_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                playlistManager.deletePlaylist(name)
                Toast.makeText(context, R.string.playlist_deleted, Toast.LENGTH_SHORT).show()
                refreshPlaylists()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openPlaylist(name: String) {
        val intent = Intent(requireContext(), PlaylistDetailActivity::class.java)
        intent.putExtra("PLAYLIST_NAME", name)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
