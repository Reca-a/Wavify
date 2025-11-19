package com.example.wavify

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.example.wavify.databinding.ItemAudioBinding

// Klasa tworząca listę utworów
class AudioAdapter(
    private val songs: List<AudioFile>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {
    inner class AudioViewHolder(val binding: ItemAudioBinding) :
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
            binding.root.setOnClickListener { onItemClick(position) }
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