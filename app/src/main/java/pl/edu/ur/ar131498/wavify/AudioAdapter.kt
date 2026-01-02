package pl.edu.ur.ar131498.wavify

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import pl.edu.ur.ar131498.wavify.databinding.ItemAudioBinding

// Klasa tworząca listę utworów
class AudioAdapter(
    private val onItemClick: (List<AudioFile>, Int) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    private var songs: List<AudioFile> = emptyList()

    fun submitList(newSongs: List<AudioFile>) {
        songs = newSongs
        notifyDataSetChanged()
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