package com.example.test;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);
    
    @Update
    void update(Note note);
    
    @Delete
    void delete(Note note);
    
    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteById(String noteId);
    
    @Query("DELETE FROM notes")
    void deleteAllNotes();
    
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    List<Note> getAllNotes();
    
    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<Note> searchNotes(String query);
}