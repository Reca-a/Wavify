package pl.edu.ur.ar131498.wavify

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pl.edu.ur.ar131498.wavify.databinding.ItemPlaylistBinding

 data class PlaylistUIModel(
    val name: String,
    val songCount: Int,
    val coverUris: List<android.net.Uri>
)

class PlaylistAdapter(
    private var playlists: List<PlaylistUIModel>,
    private val onPlaylistClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val item = playlists[position]
        holder.binding.playlistName.text = item.name

        // Ilość utworów
        val countText = holder.itemView.context.resources.getQuantityString(
            R.plurals.songs_count, item.songCount, item.songCount
        )
        holder.binding.playlistCount.text = countText

        // Okładka playlisty
        val covers = item.coverUris
        if (covers.isEmpty() || item.songCount == 0) {
            holder.binding.singleCover.visibility = VISIBLE
            holder.binding.gridContainer.visibility = GONE
            holder.binding.singleCover.setImageResource(R.drawable.ic_playlist_play_48)
            holder.binding.singleCover.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            holder.binding.singleCover.setPadding(32, 32, 32, 32)

            val typedValue = android.util.TypedValue()
            holder.itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            holder.binding.singleCover.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
            
        } else if (covers.size == 1) { // jeden utwór
            holder.binding.singleCover.visibility = VISIBLE
            holder.binding.gridContainer.visibility = GONE
            holder.binding.singleCover.setPadding(0,0,0,0)
            holder.binding.singleCover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

            holder.binding.singleCover.imageTintList = null
            
            holder.binding.singleCover.load(covers[0]) {
                placeholder(R.drawable.ic_playlist_play_48)
                error(R.drawable.ic_playlist_play_48)
            }
        } else if (covers.size == 2) { // dwa utwory
            holder.binding.singleCover.visibility = GONE
            holder.binding.gridContainer.visibility = VISIBLE

            holder.binding.cover1.load(covers.getOrNull(0)) {
                placeholder(android.R.color.transparent)
            }
            holder.binding.cover2.load(covers.getOrNull(1)) {
                placeholder(android.R.color.transparent)
            }
            holder.binding.collageRow2.visibility = GONE

            holder.itemView.setOnClickListener {
                onPlaylistClick(item.name)
            }

            holder.binding.deleteButton.setOnClickListener {
                onDeleteClick(item.name)
            }
        } else { // 3 lub więcej utworów
            holder.binding.singleCover.visibility = GONE
            holder.binding.gridContainer.visibility = VISIBLE

            holder.binding.collageRow2.visibility = VISIBLE

            holder.binding.cover1.load(covers.getOrNull(0)) {
                 placeholder(android.R.color.transparent)
            }
            holder.binding.cover2.load(covers.getOrNull(1)) {
                 placeholder(android.R.color.transparent)
            }
            holder.binding.cover3.load(covers.getOrNull(2)) {
                 placeholder(android.R.color.transparent)
            }
            holder.binding.cover4.load(covers.getOrNull(3)) {
                 placeholder(android.R.color.transparent)
            }
        }

        holder.itemView.setOnClickListener {
            onPlaylistClick(item.name)
        }

        holder.binding.deleteButton.setOnClickListener {
            onDeleteClick(item.name)
        }
    }

    override fun getItemCount(): Int = playlists.size

    fun updateData(newPlaylists: List<PlaylistUIModel>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }
}
