package pl.edu.ur.ar131498.wavify

import android.content.Context
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import coil.load
import pl.edu.ur.ar131498.wavify.data.AudioFile
import pl.edu.ur.ar131498.wavify.data.FavoritesManager
import pl.edu.ur.ar131498.wavify.data.PlaylistManager

class SongMenuBottomSheet(
    private val context: Context,
    private val song: AudioFile,
    private val currentPlaylistName: String? = null,
    private val onPlaylistChanged: (() -> Unit)? = null,
    private val onFavoriteChanged: () -> Unit
) {
    fun show() {
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(R.layout.bottom_sheet_song_menu)

        val favoritesManager = FavoritesManager(context)
        val playlistManager = PlaylistManager(context)

        dialog.findViewById<TextView>(R.id.sheetSongTitle)?.text = song.title
        val artistText = if (!song.artist.isNullOrEmpty() && song.artist != "<unknown>") song.artist else context.getString(R.string.unknown_artist)
        dialog.findViewById<TextView>(R.id.sheetSongArtist)?.text = artistText
        
        val albumArtView = dialog.findViewById<ImageView>(R.id.sheetAlbumArt)
        albumArtView?.load(song.albumArtUri) {
             placeholder(R.drawable.default_album_art)
             error(R.drawable.default_album_art)
        }

        val isFav = favoritesManager.isFavorite(song.uri.toString())
        val favAction = dialog.findViewById<TextView>(R.id.actionAddToFavorites)
        
        if (isFav) {
            favAction?.text = context.getString(R.string.remove_from_favorites)
            favAction?.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_favorite_filled_24, 0, 0, 0)
        } else {
            favAction?.text = context.getString(R.string.add_to_favorites)
            favAction?.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_favorite_border_24, 0, 0, 0)
        }

        favAction?.setOnClickListener {
            if (isFav) {
                favoritesManager.removeFavorite(song.uri.toString())
                Toast.makeText(context, R.string.remove_from_favorites, Toast.LENGTH_SHORT).show()
            } else {
                favoritesManager.addFavorite(song.uri.toString())
                Toast.makeText(context, R.string.add_to_favorites, Toast.LENGTH_SHORT).show()
            }
            onFavoriteChanged()
            dialog.dismiss()
        }

        val addToPlaylistAction = dialog.findViewById<TextView>(R.id.actionAddToPlaylist)
        addToPlaylistAction?.setOnClickListener {
            dialog.dismiss()
            AddToPlaylistBottomSheet(context, song.uri.toString()).show()
        }

        if (currentPlaylistName != null) {
            val removeAction = dialog.findViewById<TextView>(R.id.actionRemoveFromPlaylist)
            removeAction?.visibility = android.view.View.VISIBLE
            removeAction?.setOnClickListener {
                playlistManager.removeSongFromPlaylist(currentPlaylistName, song.uri.toString())
                Toast.makeText(context, R.string.song_removed_from_playlist, Toast.LENGTH_SHORT).show()
                onPlaylistChanged?.invoke()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
}
