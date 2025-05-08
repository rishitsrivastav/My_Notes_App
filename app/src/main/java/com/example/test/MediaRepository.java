package com.example.test;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MediaRepository {
    private static final String TAG = "MediaRepository";
    
    private MediaDao mediaDao;
    private Application application;
    
    public MediaRepository(Application application) {
        NoteDatabase database = NoteDatabase.getDatabase(application);
        mediaDao = database.mediaDao();
        this.application = application;
    }
    
    public boolean insert(Media media) {
        try {
            Log.d(TAG, "Inserting media: " + media.toString());
            
            // Make sure all required fields are set
            if (media.getNoteId() == null || media.getNoteId().isEmpty()) {
                Log.e(TAG, "Cannot insert media: noteId is null or empty");
                return false;
            }
            
            if (media.getUri() == null || media.getUri().isEmpty()) {
                Log.e(TAG, "Cannot insert media: uri is null or empty");
                return false;
            }
            
            // Insert into database
            mediaDao.insert(media);
            Log.d(TAG, "Media inserted successfully with ID: " + media.getId());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting media", e);
            return false;
        }
    }
    
    public void update(Media media) {
        try {
            Log.d(TAG, "Updating media: " + media.getId());
            mediaDao.update(media);
        } catch (Exception e) {
            Log.e(TAG, "Error updating media", e);
        }
    }
    
    public boolean delete(Media media) {
        try {
            Log.d(TAG, "Deleting media: " + media.getId());
            
            // Delete the actual file
            if (media.getUri() != null) {
                File mediaFile = new File(media.getUri());
                if (mediaFile.exists()) {
                    mediaFile.delete();
                }
            }
            
            // Delete the thumbnail file if it exists
            if (media.getThumbnailUri() != null) {
                File thumbnailFile = new File(media.getThumbnailUri());
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete();
                }
            }
            
            // Delete from database
            mediaDao.deleteById(media.getId());
            Log.d(TAG, "Media deleted successfully from database");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting media", e);
            return false;
        }
    }
    
    public void deleteAllMediaForNote(String noteId) {
        try {
            Log.d(TAG, "Deleting all media for note: " + noteId);
            
            // Get all media for the note
            List<Media> mediaList = getMediaForNote(noteId);
            
            // Delete each media file
            for (Media media : mediaList) {
                delete(media);
            }
            
            // Delete all from database
            mediaDao.deleteAllMediaForNote(noteId);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting all media for note", e);
        }
    }
    
    public List<Media> getMediaForNote(String noteId) {
        try {
            List<Media> mediaList = mediaDao.getMediaForNote(noteId);
            Log.d(TAG, "Retrieved " + (mediaList != null ? mediaList.size() : 0) + " media items for note: " + noteId);
            return mediaList;
        } catch (Exception e) {
            Log.e(TAG, "Error getting media for note", e);
            return null;
        }
    }
    
    public Media getMediaById(String mediaId) {
        try {
            Media media = mediaDao.getMediaById(mediaId);
            Log.d(TAG, "Retrieved media: " + (media != null ? media.getId() : "null"));
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Error getting media by id", e);
            return null;
        }
    }
    
    // Save a media file from a Uri to the app's private storage
    public Media saveMediaFromUri(Context context, Uri sourceUri, String noteId, int mediaType) {
        Log.d(TAG, "Saving media from Uri: " + sourceUri + ", noteId: " + noteId + ", mediaType: " + mediaType);
        
        try {
            ContentResolver contentResolver = context.getContentResolver();
            String mimeType = contentResolver.getType(sourceUri);
            Log.d(TAG, "Media mime type: " + mimeType);
            
            // Create a file to save the media
            File mediaFile = createMediaFile(context, mediaType);
            Log.d(TAG, "Created media file: " + mediaFile.getAbsolutePath());
            
            // Copy the content from the source Uri to our file
            try (InputStream in = contentResolver.openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(mediaFile)) {
                
                if (in == null) {
                    Log.e(TAG, "Failed to open input stream for Uri: " + sourceUri);
                    return null;
                }
                
                byte[] buffer = new byte[4096]; // Increased buffer size for better performance
                int length;
                long totalBytes = 0;
                
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                    totalBytes += length;
                }
                out.flush();
                
                Log.d(TAG, "Successfully copied " + totalBytes + " bytes to " + mediaFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Error copying file from Uri", e);
                return null;
            }
            
            // Verify the file was created successfully
            if (!mediaFile.exists() || mediaFile.length() == 0) {
                Log.e(TAG, "Media file was not created successfully or is empty");
                return null;
            }
            
            // Create a Media object
            Media media = new Media(noteId, mediaType, mediaFile.getAbsolutePath());
            Log.d(TAG, "Created Media object: " + media.toString());
            
            // Generate and save thumbnail if it's a video
            if (mediaType == Media.TYPE_VIDEO) {
                Log.d(TAG, "Generating thumbnail for video");
                File thumbnailFile = createThumbnailFile(context);
                Bitmap thumbnail = null;
                
                try {
                    // For Android 10+ (API 29+)
                    thumbnail = ThumbnailUtils.createVideoThumbnail(mediaFile, new Size(320, 240), null);
                    Log.d(TAG, "Created video thumbnail using modern API");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating video thumbnail with modern API", e);
                    // Fallback for older Android versions
                    try {
                        thumbnail = ThumbnailUtils.createVideoThumbnail(
                                mediaFile.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
                        Log.d(TAG, "Created video thumbnail using legacy API");
                    } catch (Exception e2) {
                        Log.e(TAG, "Error creating video thumbnail with legacy API", e2);
                    }
                }
                
                if (thumbnail != null) {
                    try (FileOutputStream out = new FileOutputStream(thumbnailFile)) {
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        media.setThumbnailUri(thumbnailFile.getAbsolutePath());
                        Log.d(TAG, "Saved thumbnail to: " + thumbnailFile.getAbsolutePath());
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving thumbnail", e);
                    }
                }
            }
            
            // Insert the media into the database
            Log.d(TAG, "Inserting media into database");
            if (insert(media)) {
                Log.d(TAG, "Successfully inserted media into database");
                return media;
            } else {
                Log.e(TAG, "Failed to insert media into database");
                // Clean up if database insertion failed
                if (mediaFile.exists()) {
                    boolean deleted = mediaFile.delete();
                    Log.d(TAG, "Deleted media file: " + deleted);
                }
                if (media.getThumbnailUri() != null) {
                    File thumbnailFile = new File(media.getThumbnailUri());
                    if (thumbnailFile.exists()) {
                        boolean deleted = thumbnailFile.delete();
                        Log.d(TAG, "Deleted thumbnail file: " + deleted);
                    }
                }
                return null;
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving media from Uri", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error saving media", e);
            return null;
        }
    }
    
    // Create a file for storing media
    private File createMediaFile(Context context, int mediaType) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "MEDIA_" + timeStamp + "_" + UUID.randomUUID().toString();
        
        File storageDir = new File(context.getFilesDir(), "media");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        String extension = (mediaType == Media.TYPE_IMAGE) ? ".jpg" : ".mp4";
        return new File(storageDir, fileName + extension);
    }
    
    // Create a file for storing thumbnails
    private File createThumbnailFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "THUMB_" + timeStamp + "_" + UUID.randomUUID().toString() + ".jpg";
        
        File storageDir = new File(context.getFilesDir(), "thumbnails");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        return new File(storageDir, fileName);
    }
    
    // Get a content Uri for a file (for sharing or displaying)
    public Uri getContentUri(Context context, File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }
}