package com.example.test;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "media",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "note_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("note_id")})
public class Media {
    
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    
    @NonNull
    @ColumnInfo(name = "note_id")
    private String noteId;
    
    @ColumnInfo(name = "type")
    private int type; // 1 for image, 2 for video
    
    @ColumnInfo(name = "uri")
    private String uri;
    
    @ColumnInfo(name = "thumbnail_uri")
    private String thumbnailUri;
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    public Media() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }
    
    @Ignore
    public Media(String noteId, int type, String uri) {
        this.id = UUID.randomUUID().toString();
        this.noteId = noteId;
        this.type = type;
        this.uri = uri;
        this.timestamp = System.currentTimeMillis();
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    @NonNull
    public String getNoteId() {
        return noteId;
    }
    
    public void setNoteId(@NonNull String noteId) {
        this.noteId = noteId;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getUri() {
        return uri;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String getThumbnailUri() {
        return thumbnailUri;
    }
    
    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Media{" +
                "id='" + id + '\'' +
                ", noteId='" + noteId + '\'' +
                ", type=" + type +
                ", uri='" + uri + '\'' +
                ", thumbnailUri='" + thumbnailUri + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}