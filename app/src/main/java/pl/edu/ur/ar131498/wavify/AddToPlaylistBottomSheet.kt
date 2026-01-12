package pl.edu.ur.ar131498.wavify

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import pl.edu.ur.ar131498.wavify.databinding.BottomSheetAddToPlaylistBinding
import pl.edu.ur.ar131498.wavify.databinding.ItemPlaylistSelectionBinding

class AddToPlaylistBottomSheet(
    private val context: Context,
    private val songUri: String
) {
    private val playlistManager = PlaylistManager(context)
    private val musicRepository = MusicRepository

    fun show() {
        val dialog = BottomSheetDialog(context)
        val binding = BottomSheetAddToPlaylistBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.newPlaylistButton.setOnClickListener {
            showCreatePlaylistDialog {
                refreshList(binding.playlistsRecyclerView, dialog)
            }
        }

        refreshList(binding.playlistsRecyclerView, dialog)
        dialog.show()
    }

    private fun refreshList(recyclerView: RecyclerView, dialog: BottomSheetDialog) {
        val playlists = playlistManager.getPlaylists().toList().sorted()

        val allSongs = MusicRepository.getLocalAudioFiles(context)
        val songMap = allSongs.associateBy { it.uri.toString() }

        val uiModels = playlists.map { name ->
            val songUris = playlistManager.getSongUris(name)

            val covers = songUris.mapNotNull { uri -> 
                songMap[uri]?.albumArtUri 
            }.take(4)
            
            PlaylistSelectionItem(name, songUris.size, covers)
        }

        val adapter = PlaylistSelectionAdapter(uiModels) { selectedPlaylist ->
            handlePlaylistSelection(selectedPlaylist, dialog)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun handlePlaylistSelection(playlistName: String, dialog: BottomSheetDialog) {
        if (playlistManager.isSongInPlaylist(playlistName, songUri)) {
            AlertDialog.Builder(context)
                .setTitle(R.string.add_duplicate)
                .setMessage(R.string.song_already_in_playlist)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    addSongToPlaylist(playlistName)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            addSongToPlaylist(playlistName)
            dialog.dismiss()
        }
    }

    private fun addSongToPlaylist(playlistName: String) {
        playlistManager.addSongToPlaylist(playlistName, songUri)
        Toast.makeText(context, R.string.song_added_to_playlist, Toast.LENGTH_SHORT).show()
    }

    private fun showCreatePlaylistDialog(onCreated: () -> Unit) {
        val input = EditText(context)
        input.hint = context.getString(R.string.enter_playlist_name)

        input.filters = arrayOf(android.text.InputFilter.LengthFilter(20))
        
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding / 2, padding, padding / 2)

        AlertDialog.Builder(context)
            .setTitle(R.string.new_playlist)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()

                val regex = Regex("^[a-zA-Z0-9 _-]*$")

                if (name.isNotEmpty()) {
                    if (!name.matches(regex)) {
                         Toast.makeText(context, "Nazwa zawiera niedozwolone znaki", Toast.LENGTH_SHORT).show()
                    } else if (playlistManager.createPlaylist(name)) {
                        Toast.makeText(context, R.string.playlist_created, Toast.LENGTH_SHORT).show()
                        onCreated()
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

    data class PlaylistSelectionItem(
        val name: String,
        val count: Int,
        val coverUris: List<android.net.Uri>
    )

    inner class PlaylistSelectionAdapter(
        private val items: List<PlaylistSelectionItem>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<PlaylistSelectionAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemPlaylistSelectionBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPlaylistSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.playlistName.text = item.name
            holder.binding.songCount.text = "${item.count} ${context.getString(R.string.songs).lowercase()}"

            // Handle Collage
            val covers = item.coverUris
            if (covers.isEmpty()) {
                holder.binding.singleCover.visibility = android.view.View.VISIBLE
                holder.binding.gridContainer.visibility = android.view.View.GONE
                holder.binding.singleCover.setImageResource(R.drawable.ic_playlist_play_48)
                holder.binding.singleCover.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                holder.binding.singleCover.setPadding(8, 8, 8, 8) // adjusted padding for smaller size
                
                // Restore tint for placeholder
                val typedValue = android.util.TypedValue()
                holder.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
                holder.binding.singleCover.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
                
            } else if (covers.size == 1) {
                 // 1 song, show first one big
                holder.binding.singleCover.visibility = android.view.View.VISIBLE
                holder.binding.gridContainer.visibility = android.view.View.GONE
                holder.binding.singleCover.setPadding(0,0,0,0)
                holder.binding.singleCover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                
                // Clear tint for real image
                holder.binding.singleCover.imageTintList = null
                
                holder.binding.singleCover.load(covers[0]) {
                    placeholder(R.drawable.ic_playlist_play_48)
                    error(R.drawable.ic_playlist_play_48)
                }
            } else {
                // 2+ songs, show grid
                holder.binding.singleCover.visibility = android.view.View.GONE
                holder.binding.gridContainer.visibility = android.view.View.VISIBLE
                
                holder.binding.cover1.load(covers.getOrNull(0)) { placeholder(android.R.color.transparent) }
                holder.binding.cover2.load(covers.getOrNull(1)) { placeholder(android.R.color.transparent) }
                holder.binding.cover3.load(covers.getOrNull(2)) { placeholder(android.R.color.transparent) }
                holder.binding.cover4.load(covers.getOrNull(3)) { placeholder(android.R.color.transparent) }
            }

            holder.itemView.setOnClickListener { onClick(item.name) }
        }

        override fun getItemCount() = items.size
    }
}
