package com.example.test;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MediaDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Media media);
    
    @Update
    void update(Media media);
    
    @Delete
    void delete(Media media);
    
    @Query("DELETE FROM media WHERE id = :mediaId")
    void deleteById(String mediaId);
    
    @Query("DELETE FROM media WHERE note_id = :noteId")
    void deleteAllMediaForNote(String noteId);
    
    @Query("SELECT * FROM media WHERE note_id = :noteId ORDER BY timestamp ASC")
    List<Media> getMediaForNote(String noteId);
    
    @Query("SELECT * FROM media WHERE id = :mediaId")
    Media getMediaById(String mediaId);
}