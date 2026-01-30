package pl.edu.ur.ar131498.wavify.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.edu.ur.ar131498.wavify.R

class AuthorsAdapter(
    private val onAuthorClick: (String) -> Unit
) : RecyclerView.Adapter<AuthorsAdapter.AuthorViewHolder>() {

    private var authors: List<String> = emptyList()

    fun submitList(newAuthors: List<String>) {
        authors = newAuthors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_author, parent, false)
        return AuthorViewHolder(view)
    }

    override fun onBindViewHolder(holder: AuthorViewHolder, position: Int) {
        holder.bind(authors[position])
    }

    override fun getItemCount(): Int = authors.size

    inner class AuthorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.authorName)

        fun bind(author: String) {
            nameText.text = author
            itemView.setOnClickListener { onAuthorClick(author) }
        }
    }
}
