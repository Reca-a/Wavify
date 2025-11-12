package com.example.wavify

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
            binding.root.setOnClickListener { onItemClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding = ItemAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(songs[position], position)
    }

    override fun getItemCount() = songs.size
}