package com.example.calculatorvault.vault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calculatorvault.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultNoteAdapter(
    private var notes: List<VaultNote>,
    private val onClick: (VaultNote) -> Unit,
    private val onLongClick: (VaultNote) -> Unit
) : RecyclerView.Adapter<VaultNoteAdapter.NoteViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val preview: TextView = view.findViewById(R.id.tvPreview)
        val date: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vault_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.preview.text = note.preview.ifBlank { "(kosong)" }
        holder.date.text = dateFormat.format(Date(note.createdAtMillis))
        holder.itemView.setOnClickListener { onClick(note) }
        holder.itemView.setOnLongClickListener { onLongClick(note); true }
    }

    override fun getItemCount(): Int = notes.size

    fun submitList(newNotes: List<VaultNote>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}
