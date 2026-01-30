package pl.edu.ur.ar131498.wavify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import pl.edu.ur.ar131498.wavify.data.AudioFile
import pl.edu.ur.ar131498.wavify.data.FavoritesManager

enum class SortOrder {
    TITLE_ASC, TITLE_DESC,
    ARTIST_ASC, ARTIST_DESC,
    DATE_DESC, DATE_ASC
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesManager = FavoritesManager(application)

    private val _songs = MutableLiveData<List<AudioFile>>(emptyList())
    val songs: LiveData<List<AudioFile>> = _songs

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery
    
    private val _sortOrder = MutableLiveData<SortOrder>(SortOrder.DATE_DESC)
    val sortOrder: LiveData<SortOrder> = _sortOrder
    
    fun loadSongs(list: List<AudioFile>) {
        _songs.value = sortList(list, _sortOrder.value ?: SortOrder.DATE_DESC)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        _songs.value = sortList(_songs.value ?: emptyList(), order)
        refreshFavorites()
    }
    
    private fun sortList(list: List<AudioFile>, order: SortOrder): List<AudioFile> {
        return when (order) {
            SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOrder.ARTIST_ASC -> list.sortedBy { (it.artist ?: "").lowercase() }
            SortOrder.ARTIST_DESC -> list.sortedByDescending { (it.artist ?: "").lowercase() }
            SortOrder.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> list.sortedBy { it.dateAdded }
        }
    }
    
    private fun getFavorites(): List<AudioFile> {
        val allSongs = _songs.value ?: emptyList()
        val favs = favoritesManager.getFavorites()
        return allSongs.filter { favs.contains(it.uri.toString()) }
    }
    
    fun refreshFavorites() {
       updateFavoritesLiveData()
    }
    
    private val _favorites = MutableLiveData<List<AudioFile>>()
    val favorites: LiveData<List<AudioFile>> = _favorites
    
    private fun updateFavoritesLiveData() {
        _favorites.value = getFavorites()
    }
}
