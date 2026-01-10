package pl.edu.ur.ar131498.wavify

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import pl.edu.ur.ar131498.wavify.databinding.ItemAudioBinding

// Klasa tworząca listę utworów
class AudioAdapter(
    private val onItemClick: (List<AudioFile>, Int) -> Unit,
    private val onFavoriteClick: ((AudioFile) -> Unit)? = null
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    private var songs: List<AudioFile> = emptyList()

    fun submitList(newSongs: List<AudioFile>) {
        songs = newSongs
        notifyDataSetChanged()
    }
    
    private lateinit var favoritesManager: FavoritesManager

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        favoritesManager = FavoritesManager(recyclerView.context)
    }

    inner class AudioViewHolder(private val binding: ItemAudioBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: AudioFile, position: Int) {
            binding.titleText.text = song.title
            binding.artistText.text = if (song.artist != null && song.artist != "<unknown>") song.artist else "Nieznany artysta"
            binding.albumArtImageView.load(song.albumArtUri) {
                placeholder(R.drawable.default_album_art)
                error(R.drawable.default_album_art)
                crossfade(true)
                memoryCachePolicy(CachePolicy.ENABLED)
                diskCachePolicy(CachePolicy.ENABLED)
            }
            
            // Favorite logic
            val isFav = favoritesManager.isFavorite(song.uri.toString())
            binding.favoriteButton.setImageResource(
                if (isFav) R.drawable.ic_favorite_filled_24 else R.drawable.ic_favorite_border_24
            )
            binding.favoriteButton.setOnClickListener {
                if (isFav) {
                    favoritesManager.removeFavorite(song.uri.toString())
                } else {
                    favoritesManager.addFavorite(song.uri.toString())
                }
                notifyItemChanged(position)
                onFavoriteClick?.invoke(song)
            }

            binding.root.setOnClickListener { onItemClick(songs, position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding = ItemAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    // Ripple effect podczas klikania w utwór na liście
    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(songs[position], position)
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    override fun getItemCount() = songs.size
}