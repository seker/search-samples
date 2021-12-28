/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.example.appsearchsample

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.example.appsearchsample.databinding.ActivityMainBinding
import com.android.example.appsearchsample.model.Note
import com.android.example.appsearchsample.model.NoteViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * Activity to set up a simple AppSearch demo.
 *
 * This activity allows the user to add a note by inputting their desired text
 * into the dialog prompt. Once a note is added, it is indexed into AppSearch.
 * The user can then use the search bar to put input terms to find notes that
 * match.
 *
 * By default, the notes list displays all notes that have been indexed. Once
 * the user submits a query, the list is updated to reflect notes that match
 * the query.
 */
class MainActivity : AppCompatActivity() {
  private val noteViewModel: NoteViewModel by viewModels {
    NoteViewModel.NoteViewModelFactory(application)
  }

  private lateinit var activityBinding: ActivityMainBinding
  private lateinit var searchView: SearchView
  private lateinit var notesAdapter: NoteListItemAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activityBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(activityBinding.root)

    initAddNoteButtonListener()
    initNoteListView()

    noteViewModel.queryNotes().observe(
      this,
      {
        notesAdapter.submitList(it.map { it2 -> it2.genericDocument.toDocumentClass(Note::class.java) })
        activityBinding.progressSpinner.visibility = View.GONE
        if (it.isEmpty()) {
          activityBinding.notesList.visibility = View.GONE
          activityBinding.noNotesMessage.visibility = View.VISIBLE
        } else {
          activityBinding.notesList.visibility = View.VISIBLE
          activityBinding.noNotesMessage.visibility = View.GONE
        }
      }
    )

    noteViewModel.errorMessageLiveData.observe(this, {
      it?.let {
        Toast.makeText(applicationContext, it, LENGTH_LONG).show()
      }
    })
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.options_menu, menu)
    searchView = menu.findItem(R.id.search_bar).actionView as SearchView

    initQueryListener()

    return true
  }

  /** Initializes listeners for query input. */
  private fun initQueryListener() {
    searchView.queryHint = getString(R.string.search_bar_hint)
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean {
        noteViewModel.queryNotes(query)
        notesAdapter.query = query
        return false
      }

      override fun onQueryTextChange(newText: String): Boolean {
        // This resets the notes list to display all notes if the query is
        // cleared.
        if (newText.isEmpty()) noteViewModel.queryNotes()
        return false
      }
    })
  }

  /**
   * Initializes listener for insert note button.
   *
   * The listener configures an alert dialog for the user to input text
   * to save as a [Note] document.
   */
  private fun initAddNoteButtonListener() {
    val insertNoteButton: ExtendedFloatingActionButton =
      activityBinding.insertNoteButton

    insertNoteButton.setOnClickListener {
      val dialogBuilder = AlertDialog.Builder(this@MainActivity)
      dialogBuilder.setView(R.layout.add_note_dialog)
        .setCancelable(false)
        .setPositiveButton(R.string.add_note_dialog_save) { dialog, _ ->
          val addNoteDialogView = dialog as AlertDialog
          val noteEditText =
            addNoteDialogView.findViewById(R.id.add_note_text) as EditText?
          val noteText = noteEditText?.text.toString()
          activityBinding.progressSpinner.visibility = View.VISIBLE
          activityBinding.noNotesMessage.visibility = View.GONE
          activityBinding.notesList.visibility = View.GONE
          noteViewModel.addNote(noteText)
        }
        .setNegativeButton(R.string.add_note_dialog_cancel) { dialog, _ ->
          dialog.cancel()
        }
      val alert = dialogBuilder.create()
      alert.setTitle(R.string.add_note_dialog_title)
      alert.show()
    }
  }

  /** Initializes recycler view for list of [Note] documents. */
  private fun initNoteListView() {
    notesAdapter = NoteListItemAdapter {
      if (it != null) {
        noteViewModel.removeNote(it.namespace, it.id)
      }
    }
    activityBinding.notesList.adapter = notesAdapter
    activityBinding.notesList.addItemDecoration(
      DividerItemDecoration(
        this,
        LinearLayoutManager.VERTICAL
      )
    )
    activityBinding.notesList.layoutManager = LinearLayoutManager(this)
  }
}
