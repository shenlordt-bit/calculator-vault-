package com.example.calculatorvault.vault

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculatorvault.MainActivity
import com.example.calculatorvault.R
import com.example.calculatorvault.databinding.ActivityVaultBinding
import com.example.calculatorvault.security.CryptoManager
import com.example.calculatorvault.ui.ChangePinActivity
import com.example.calculatorvault.util.VaultSession

class VaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaultBinding
    private lateinit var repository: VaultRepository
    private lateinit var adapter: VaultNoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Requirement: vault content must never appear in the recent-apps
        // (task switcher) preview, and screenshots inside the vault are
        // blocked too. FLAG_SECURE must be set before setContentView.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repository = VaultRepository(CryptoManager(applicationContext))
        adapter = VaultNoteAdapter(
            notes = emptyList(),
            onClick = { note -> showNoteContent(note) },
            onLongClick = { note -> confirmDelete(note) }
        )
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter

        binding.fabAdd.setOnClickListener { showAddNoteDialog() }
    }

    override fun onResume() {
        super.onResume()
        // Guard against re-entering the vault via "back"/recents after the
        // process was backgrounded and VaultSession got locked. This is the
        // per-Activity half of the auto-lock mechanism described in
        // VaultApplication (which handles the process-wide trigger).
        if (!VaultSession.isUnlocked()) {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
            return
        }
        refreshList()
    }

    override fun onPause() {
        super.onPause()
        // Belt-and-suspenders: also lock as soon as this screen itself is
        // no longer in the foreground (covers task-switcher / power button).
        VaultSession.lock()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_vault, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_pin -> {
                startActivity(Intent(this, ChangePinActivity::class.java))
                true
            }
            R.id.action_lock_now -> {
                VaultSession.lock()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshList() {
        val notes = repository.listNotes()
        adapter.submitList(notes)
        binding.tvEmpty.visibility = if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerNotes.visibility = if (notes.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun showAddNoteDialog() {
        val input = EditText(this).apply { hint = getString(R.string.hint_note_content) }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_note_title)
            .setView(input)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val text = input.text.toString()
                if (text.isNotBlank()) {
                    repository.addNote(text)
                    refreshList()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showNoteContent(note: VaultNote) {
        val content = repository.readNoteContent(note.fileName)
        AlertDialog.Builder(this)
            .setMessage(content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun confirmDelete(note: VaultNote) {
        AlertDialog.Builder(this)
            .setMessage("Hapus catatan ini?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                repository.deleteNote(note.fileName)
                refreshList()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
