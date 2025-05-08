package com.example.test;

import android.app.Application;
import android.util.Log;

public class NoteApplication extends Application {
    private static final String TAG = "NoteApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Initializing application and database");
        
        // Initialize the database
        try {
            NoteDatabase database = NoteDatabase.getDatabase(this);
            Log.d(TAG, "Database initialized successfully: " + database);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
        }
    }
}