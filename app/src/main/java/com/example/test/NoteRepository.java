package com.example.test;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class NoteRepository {
    private static final String TAG = "NoteRepository";
    private NoteDao noteDao;
    private NoteDatabase database;
    
    public NoteRepository(Application application) {
        database = NoteDatabase.getDatabase(application);
        noteDao = database.noteDao();
    }
    
    public boolean insert(Note note) {
        try {
            Log.d(TAG, "Inserting note: " + note.getContent());
            noteDao.insert(note);
            Log.d(TAG, "Note inserted successfully into database: " + note.getId());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting note", e);
            return false;
        }
    }
    
    public void update(Note note) {
        try {
            Log.d(TAG, "Updating note: " + note.getContent());
            noteDao.update(note);
        } catch (Exception e) {
            Log.e(TAG, "Error updating note", e);
        }
    }
    
    public boolean delete(Note note) {
        try {
            Log.d(TAG, "Deleting note: " + note.getContent() + " with ID: " + note.getId());
            // Use deleteById for more reliable deletion
            noteDao.deleteById(note.getId());
            Log.d(TAG, "Note deleted successfully from database");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting note", e);
            return false;
        }
    }
    
    public void deleteAllNotes() {
        try {
            Log.d(TAG, "Deleting all notes");
            noteDao.deleteAllNotes();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting all notes", e);
        }
    }
    
    public List<Note> getAllNotes() {
        try {
            List<Note> notes = noteDao.getAllNotes();
            Log.d(TAG, "Retrieved " + (notes != null ? notes.size() : 0) + " notes");
            return notes;
        } catch (Exception e) {
            Log.e(TAG, "Error getting all notes", e);
            return null;
        }
    }
    
    public List<Note> searchNotes(String query) {
        try {
            Log.d(TAG, "Searching notes with query: " + query);
            List<Note> notes = noteDao.searchNotes(query);
            Log.d(TAG, "Search found " + (notes != null ? notes.size() : 0) + " notes");
            return notes;
        } catch (Exception e) {
            Log.e(TAG, "Error searching notes", e);
            return null;
        }
    }
}