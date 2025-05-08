package com.example.test;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Note.class, Media.class}, version = 2, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {
    
    private static NoteDatabase instance;
    
    public abstract NoteDao noteDao();
    public abstract MediaDao mediaDao();
    
    // Migration from version 1 to 2 (adding media table)
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new media table
            database.execSQL("CREATE TABLE IF NOT EXISTS `media` (" +
                    "`id` TEXT NOT NULL, " +
                    "`note_id` TEXT NOT NULL, " +
                    "`type` INTEGER NOT NULL, " +
                    "`uri` TEXT, " +
                    "`thumbnail_uri` TEXT, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            
            // Create index for note_id
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_note_id` ON `media` (`note_id`)");
        }
    };
    
    public static synchronized NoteDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    NoteDatabase.class,
                    "note_database")
                    .allowMainThreadQueries() // Allow queries on the main thread (for simplicity)
                    .addMigrations(MIGRATION_1_2) // Add migration strategy
                    .build();
        }
        return instance;
    }
}