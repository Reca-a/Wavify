package pl.edu.ur.ar131498.wavify

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _songs = MutableLiveData<List<AudioFile>>(emptyList())
    val songs: LiveData<List<AudioFile>> = _songs

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    fun loadSongs(list: List<AudioFile>) {
        _songs.value = list
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
