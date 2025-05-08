package com.example.test;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteAdapter.NoteListener {
    private static final String TAG = "MainActivity";
    
    private RecyclerView notesRecyclerView;
    private NoteAdapter noteAdapter;
    private LinearLayout emptyStateContainer;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private List<Note> notesList;
    private NoteRepository noteRepository;
    private ExtendedFloatingActionButton addNoteFab;

    private static final int PERMISSION_REQUEST_CODE = 1003;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "onCreate: Initializing MainActivity");
        
        // Request permissions
        requestPermissions();
        
        // Initialize repository
        try {
            noteRepository = new NoteRepository(getApplication());
            Log.d(TAG, "Repository initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing repository", e);
            Toast.makeText(this, "Error initializing database", Toast.LENGTH_LONG).show();
        }
        
        // Setup edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        addNoteFab = findViewById(R.id.addNoteFab);

        // Setup RecyclerView with animation
        setupRecyclerView();
        
        // Setup search functionality
        setupSearchFunctionality();

        // Set click listener for FAB with animation
        addNoteFab.setOnClickListener(v -> {
            // Animate the FAB
            animateFab();
            // Show the dialog
            showAddEditNoteDialog(null, -1);
        });

        // Load notes from database
        loadNotesFromDb();
        
        Log.d(TAG, "onCreate: MainActivity initialization complete");
    }
    
    private void setupRecyclerView() {
        notesList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        notesRecyclerView.setLayoutManager(layoutManager);
        notesRecyclerView.setAdapter(noteAdapter);
        
        // Add scroll listener to hide/show FAB
        notesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // Scrolling down - hide FAB
                    addNoteFab.shrink();
                } else if (dy < 0) {
                    // Scrolling up - show FAB
                    addNoteFab.extend();
                }
            }
        });
    }
    
    private void setupSearchFunctionality() {
        // Add text change listener to search EditText
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter notes based on search query
                noteAdapter.getFilter().filter(s);
                
                // Show/hide clear button based on text
                if (s.length() > 0) {
                    if (clearSearchButton.getVisibility() != View.VISIBLE) {
                        clearSearchButton.setVisibility(View.VISIBLE);
                        // Animate the clear button appearance
                        clearSearchButton.setAlpha(0f);
                        clearSearchButton.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start();
                    }
                } else {
                    clearSearchButton.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    clearSearchButton.setVisibility(View.GONE);
                                }
                            })
                            .start();
                }
                
                // Update empty view based on search results
                updateEmptyView();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
        
        // Set click listener for clear button
        clearSearchButton.setOnClickListener(v -> {
            // Clear search text with animation
            searchEditText.setText("");
            
            // Give focus back to search field
            searchEditText.requestFocus();
        });
    }
    
    private void animateFab() {
        // Scale down and up animation
        addNoteFab.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> 
                    addNoteFab.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start()
                )
                .start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Refreshing notes");
        // Refresh notes when activity resumes
        loadNotesFromDb();
    }
    
    private void loadNotesFromDb() {
        try {
            Log.d(TAG, "loadNotesFromDb: Loading notes from database");
            
            // Clear the current list to avoid duplicates
            if (notesList != null) {
                notesList.clear();
            } else {
                notesList = new ArrayList<>();
            }
            
            // Get fresh data from database
            List<Note> notes = noteRepository.getAllNotes();
            
            if (notes != null && !notes.isEmpty()) {
                Log.d(TAG, "loadNotesFromDb: Loaded " + notes.size() + " notes");
                
                // Add all notes from database to our list
                notesList.addAll(notes);
                
                // Log all notes for debugging
                for (Note note : notesList) {
                    Log.d(TAG, "Note in list: " + note.getId() + " - " + note.getContent());
                }
            } else {
                Log.w(TAG, "loadNotesFromDb: No notes loaded from database");
            }
            
            // Update the adapter with the current list
            noteAdapter.setNotes(notesList);
            
            // Clear any search filter
            searchEditText.setText("");
            
            // Update empty view
            updateEmptyView();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading notes", e);
            Toast.makeText(this, "Error loading notes", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void searchNotes(String query) {
        try {
            Log.d(TAG, "searchNotes: Searching for: " + query);
            
            if (query == null || query.trim().isEmpty()) {
                // If query is empty, load all notes
                loadNotesFromDb();
                return;
            }
            
            // Search notes in database
            List<Note> searchResults = noteRepository.searchNotes(query);
            
            if (searchResults != null && !searchResults.isEmpty()) {
                Log.d(TAG, "searchNotes: Found " + searchResults.size() + " notes");
                
                // Update adapter with search results
                noteAdapter.setNotes(searchResults);
            } else {
                Log.w(TAG, "searchNotes: No notes found for query: " + query);
                
                // Clear adapter if no results
                noteAdapter.setNotes(new ArrayList<>());
            }
            
            // Update empty view
            updateEmptyView();
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching notes", e);
            Toast.makeText(this, "Error searching notes", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmptyView() {
        if (noteAdapter.getItemCount() == 0) {
            // Show empty state with animation
            if (emptyStateContainer.getVisibility() != View.VISIBLE) {
                emptyStateContainer.setAlpha(0f);
                emptyStateContainer.setVisibility(View.VISIBLE);
                emptyStateContainer.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
            }
            
            // Hide RecyclerView
            notesRecyclerView.setVisibility(View.GONE);
            
            // Update empty text based on search state
            TextView emptyText = findViewById(R.id.emptyNotesText);
            if (searchEditText.getText().length() > 0) {
                emptyText.setText("No notes found for your search\nTry a different search term");
            } else {
                emptyText.setText("No notes yet\nTap + to add a new note");
            }
        } else {
            // Hide empty state with animation
            if (emptyStateContainer.getVisibility() == View.VISIBLE) {
                emptyStateContainer.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                emptyStateContainer.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
            
            // Show RecyclerView
            notesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddEditNoteDialog(Note initialNote, int position) {
        Log.d(TAG, "showAddEditNoteDialog: " + (initialNote == null ? "Adding new note" : "Editing note: " + initialNote.getId()));
        
        // Create a wrapper class to hold the note reference that can be modified in lambdas
        class NoteWrapper {
            Note note;
            
            NoteWrapper(Note note) {
                this.note = note;
            }
        }
        
        // Initialize the wrapper with the initial note
        NoteWrapper noteWrapper = new NoteWrapper(initialNote);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_note_with_media, null);

        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText noteEditText = dialogView.findViewById(R.id.noteEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button addPhotoButton = dialogView.findViewById(R.id.addPhotoButton);
        Button addVideoButton = dialogView.findViewById(R.id.addVideoButton);
        RecyclerView mediaRecyclerView = dialogView.findViewById(R.id.mediaRecyclerView);
        
        // Create the adapter first
        final MediaAdapter mediaAdapter = new MediaAdapter(this, null);
        
        // Now set the listener with a reference to the already created adapter
        mediaAdapter.setMediaListener(new MediaAdapter.MediaListener() {
            @Override
            public void onMediaClick(Media media, int mediaPosition) {
                // Open media viewer
                Intent intent = new Intent(MainActivity.this, MediaViewerActivity.class);
                intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_URI, media.getUri());
                intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, media.getType());
                startActivity(intent);
            }
            
            @Override
            public void onMediaDelete(Media media, int mediaPosition) {
                // Delete media
                MediaRepository mediaRepository = new MediaRepository(getApplication());
                if (mediaRepository.delete(media)) {
                    // Remove from adapter
                    mediaAdapter.removeMedia(mediaPosition);
                    Toast.makeText(MainActivity.this, "Media deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error deleting media", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        mediaRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaRecyclerView.setAdapter(mediaAdapter);
        
        // Load existing media if editing a note
        if (noteWrapper.note != null) {
            MediaRepository mediaRepository = new MediaRepository(getApplication());
            List<Media> mediaList = mediaRepository.getMediaForNote(noteWrapper.note.getId());
            if (mediaList != null && !mediaList.isEmpty()) {
                mediaAdapter.setMediaList(mediaList);
            }
        }

        // Set dialog title and content based on whether we're adding or editing
        if (noteWrapper.note != null) {
            dialogTitle.setText("Edit Note");
            noteEditText.setText(noteWrapper.note.getContent());
        } else {
            dialogTitle.setText("Add New Note");
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
        
        // Apply circular reveal animation to the dialog
        View dialogRoot = dialogView.getRootView();
        if (dialogRoot != null) {
            dialogRoot.post(() -> {
                int centerX = dialogRoot.getWidth() / 2;
                int centerY = dialogRoot.getHeight() / 2;
                float finalRadius = (float) Math.hypot(centerX, centerY);
                
                Animator circularReveal = ViewAnimationUtils.createCircularReveal(
                        dialogRoot, centerX, centerY, 0f, finalRadius);
                circularReveal.setDuration(300);
                circularReveal.setInterpolator(new AccelerateDecelerateInterpolator());
                circularReveal.start();
            });
        }
        
        // Set click listeners for media buttons
        addPhotoButton.setOnClickListener(v -> {
            animateButtonClick(v);
            
            // Check if we have a note ID (for editing) or need to create one first
            if (noteWrapper.note == null) {
                // We need to create a note first
                String content = noteEditText.getText().toString().trim();
                if (content.isEmpty()) {
                    // Shake animation for empty input
                    ObjectAnimator
                            .ofFloat(noteEditText, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0)
                            .setDuration(500)
                            .start();
                    Toast.makeText(MainActivity.this, "Please enter note text first", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create a new note and save it to the database first
                Note newNote = new Note(content);
                Log.d(TAG, "Creating new note for media: " + newNote.getId());
                
                if (noteRepository.insert(newNote)) {
                    // Show image picker with the saved note ID
                    pickImage(newNote.getId(), mediaAdapter);
                    
                    // Update the dialog title to reflect we're now editing
                    dialogTitle.setText("Edit Note");
                    
                    // Set the note variable to the new note so subsequent operations use it
                    noteWrapper.note = newNote;
                    
                    Toast.makeText(MainActivity.this, "Note created, adding image...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error creating note", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Use existing note ID
                pickImage(noteWrapper.note.getId(), mediaAdapter);
            }
        });
        
        addVideoButton.setOnClickListener(v -> {
            animateButtonClick(v);
            
            // Check if we have a note ID (for editing) or need to create one first
            if (noteWrapper.note == null) {
                // We need to create a note first
                String content = noteEditText.getText().toString().trim();
                if (content.isEmpty()) {
                    // Shake animation for empty input
                    ObjectAnimator
                            .ofFloat(noteEditText, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0)
                            .setDuration(500)
                            .start();
                    Toast.makeText(MainActivity.this, "Please enter note text first", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create a new note and save it to the database first
                Note newNote = new Note(content);
                Log.d(TAG, "Creating new note for media: " + newNote.getId());
                
                if (noteRepository.insert(newNote)) {
                    // Show video picker with the saved note ID
                    pickVideo(newNote.getId(), mediaAdapter);
                    
                    // Update the dialog title to reflect we're now editing
                    dialogTitle.setText("Edit Note");
                    
                    // Set the note variable to the new note so subsequent operations use it
                    noteWrapper.note = newNote;
                    
                    Toast.makeText(MainActivity.this, "Note created, adding video...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error creating note", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Use existing note ID
                pickVideo(noteWrapper.note.getId(), mediaAdapter);
            }
        });

        // Set click listeners for buttons with animations
        cancelButton.setOnClickListener(v -> {
            // Add button click animation
            animateButtonClick(v);
            
            // If we created a note just for adding media but then cancel, delete it
            if (noteWrapper.note != null && mediaAdapter.getItemCount() == 0) {
                // Check if this is a newly created note (not in the adapter)
                boolean isNewNote = true;
                for (int i = 0; i < noteAdapter.getItemCount(); i++) {
                    if (noteAdapter.getNoteAt(i).getId().equals(noteWrapper.note.getId())) {
                        isNewNote = false;
                        break;
                    }
                }
                
                if (isNewNote) {
                    Log.d(TAG, "Deleting newly created note that was canceled: " + noteWrapper.note.getId());
                    noteRepository.delete(noteWrapper.note);
                }
            }
            
            // Dismiss dialog with circular hide animation
            dismissDialogWithAnimation(dialog, dialogView);
        });

        saveButton.setOnClickListener(v -> {
            // Add button click animation
            animateButtonClick(v);
            
            String content = noteEditText.getText().toString().trim();
            if (!content.isEmpty()) {
                try {
                    if (noteWrapper.note != null) {
                        // Edit existing note
                        Log.d(TAG, "Updating note: " + noteWrapper.note.getId());
                        noteWrapper.note.setContent(content);
                        noteWrapper.note.setTimestamp(System.currentTimeMillis());
                        noteRepository.update(noteWrapper.note);
                        
                        // Reload notes to ensure we have the latest data
                        loadNotesFromDb();
                        
                        Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                    } else {
                        // Add new note
                        Note newNote = new Note(content);
                        Log.d(TAG, "Adding new note: " + newNote.getId());
                        
                        // First insert into database
                        boolean success = noteRepository.insert(newNote);
                        
                        if (success) {
                            // Only update UI if database insertion was successful
                            Log.d(TAG, "Successfully added note to database, updating UI");
                            
                            // Clear the list and reload from database to ensure consistency
                            loadNotesFromDb();
                            
                            Toast.makeText(MainActivity.this, "Note added", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Error adding note", Toast.LENGTH_SHORT).show();
                        }
                    }
                    updateEmptyView();
                    
                    // Dismiss dialog with animation
                    dismissDialogWithAnimation(dialog, dialogView);
                } catch (Exception e) {
                    Log.e(TAG, "Error saving note", e);
                    Toast.makeText(MainActivity.this, "Error saving note", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            } else {
                // Shake animation for empty input
                ObjectAnimator
                        .ofFloat(noteEditText, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0)
                        .setDuration(500)
                        .start();
                Toast.makeText(MainActivity.this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void dismissDialogWithAnimation(AlertDialog dialog, View dialogView) {
        View dialogRoot = dialogView.getRootView();
        if (dialogRoot != null) {
            int centerX = dialogRoot.getWidth() / 2;
            int centerY = dialogRoot.getHeight() / 2;
            float initialRadius = (float) Math.hypot(centerX, centerY);
            
            Animator circularReveal = ViewAnimationUtils.createCircularReveal(
                    dialogRoot, centerX, centerY, initialRadius, 0f);
            circularReveal.setDuration(300);
            circularReveal.setInterpolator(new AccelerateDecelerateInterpolator());
            
            circularReveal.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dialog.dismiss();
                }
            });
            
            circularReveal.start();
        } else {
            dialog.dismiss();
        }
    }
    
    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> 
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                )
                .start();
    }

    private void showDeleteConfirmationDialog(Note note, int position) {
        Log.d(TAG, "showDeleteConfirmationDialog: Confirming deletion of note: " + note.getId());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialogInterface, which) -> {
                    try {
                        Log.d(TAG, "Deleting note: " + note.getId());
                        
                        // Delete from database first
                        boolean success = noteRepository.delete(note);
                        
                        if (success) {
                            Log.d(TAG, "Database deletion successful, updating UI");
                            
                            // Reload notes from database to ensure UI consistency
                            loadNotesFromDb();
                            
                            Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Database deletion failed");
                            Toast.makeText(MainActivity.this, "Error deleting note", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting note", e);
                        Toast.makeText(MainActivity.this, "Error deleting note", Toast.LENGTH_SHORT).show();
                        // Refresh the list to ensure UI consistency
                        loadNotesFromDb();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.show();
        
        // Apply animation to dialog buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnTouchListener((v, event) -> {
            animateButtonClick(v);
            return false;
        });
        
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnTouchListener((v, event) -> {
            animateButtonClick(v);
            return false;
        });
    }

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_VIDEO_PICK = 1002;
    private String pendingNoteId;
    private MediaAdapter pendingMediaAdapter;
    
    private void pickImage(String noteId, MediaAdapter mediaAdapter) {
        Log.d(TAG, "Picking image for noteId: " + noteId);
        this.pendingNoteId = noteId;
        this.pendingMediaAdapter = mediaAdapter;
        
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_IMAGE_PICK);
        } catch (Exception e) {
            Log.e(TAG, "Error launching image picker", e);
            Toast.makeText(this, "Error launching image picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void pickVideo(String noteId, MediaAdapter mediaAdapter) {
        Log.d(TAG, "Picking video for noteId: " + noteId);
        this.pendingNoteId = noteId;
        this.pendingMediaAdapter = mediaAdapter;
        
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_VIDEO_PICK);
        } catch (Exception e) {
            Log.e(TAG, "Error launching video picker", e);
            Toast.makeText(this, "Error launching video picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                // Handle image selection
                handleMediaSelection(data.getData(), Media.TYPE_IMAGE);
            } else if (requestCode == REQUEST_VIDEO_PICK) {
                // Handle video selection
                handleMediaSelection(data.getData(), Media.TYPE_VIDEO);
            }
        }
    }
    
    private void handleMediaSelection(android.net.Uri uri, int mediaType) {
        Log.d(TAG, "Handling media selection: " + uri + ", type: " + mediaType);
        
        if (pendingNoteId == null || pendingMediaAdapter == null) {
            Log.e(TAG, "Cannot handle media selection: pendingNoteId or pendingMediaAdapter is null");
            Toast.makeText(this, "Error adding media: internal error", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (uri == null) {
            Log.e(TAG, "Cannot handle media selection: uri is null");
            Toast.makeText(this, "Error adding media: invalid media", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Verify we can access the URI
            getContentResolver().getType(uri);
            
            // Save media to app's private storage
            MediaRepository mediaRepository = new MediaRepository(getApplication());
            Log.d(TAG, "Saving media from URI to storage...");
            Media media = mediaRepository.saveMediaFromUri(this, uri, pendingNoteId, mediaType);
            
            if (media != null) {
                // Add to adapter
                Log.d(TAG, "Media saved successfully, adding to adapter");
                pendingMediaAdapter.addMedia(media);
                Toast.makeText(this, mediaType == Media.TYPE_IMAGE ? "Image added" : "Video added", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to save media from URI");
                Toast.makeText(this, "Error adding media: could not save file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling media selection", e);
            Toast.makeText(this, "Error adding media: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNoteEdit(Note note, int position) {
        showAddEditNoteDialog(note, position);
    }

    @Override
    public void onNoteDelete(Note note, int position) {
        showDeleteConfirmationDialog(note, position);
    }
    
    private void requestPermissions() {
        Log.d(TAG, "Checking and requesting permissions");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+)
            boolean needsImagePermission = checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED;
            boolean needsVideoPermission = checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED;
            
            if (needsImagePermission || needsVideoPermission) {
                Log.d(TAG, "Needs permissions for Android 13+");
                
                // Check if we should show rationale for any permission
                boolean shouldShowImageRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES);
                boolean shouldShowVideoRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VIDEO);
                
                if (shouldShowImageRationale || shouldShowVideoRationale) {
                    // Show dialog explaining why we need permissions
                    showPermissionRationaleDialog(() -> {
                        // Request permissions after user acknowledges
                        requestPermissions(new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                        }, PERMISSION_REQUEST_CODE);
                    });
                } else {
                    // Request permissions directly
                    requestPermissions(new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                    }, PERMISSION_REQUEST_CODE);
                }
            } else {
                Log.d(TAG, "All permissions already granted for Android 13+");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6-12
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Needs storage permission for Android 6-12");
                
                // Check if we should show rationale
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Show dialog explaining why we need permissions
                    showPermissionRationaleDialog(() -> {
                        // Request permissions after user acknowledges
                        requestPermissions(new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, PERMISSION_REQUEST_CODE);
                    });
                } else {
                    // Request permissions directly
                    requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, PERMISSION_REQUEST_CODE);
                }
            } else {
                Log.d(TAG, "Storage permission already granted for Android 6-12");
            }
        } else {
            Log.d(TAG, "Running on Android < 6, permissions granted at install time");
        }
    }
    
    private void showPermissionRationaleDialog(Runnable onPositiveAction) {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs access to your media files to add photos and videos to your notes. Without these permissions, you won't be able to attach media to your notes.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    if (onPositiveAction != null) {
                        onPositiveAction.run();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Media features will be limited without permissions", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = grantResults.length > 0;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "All permissions granted");
                Toast.makeText(this, "Permissions granted! You can now add photos and videos to your notes.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Some permissions denied");
                // Show a dialog explaining the consequences and offering to open settings
                showPermissionDeniedDialog();
            }
        }
    }
    
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Media permissions are required to add photos and videos to your notes. Would you like to open settings to grant these permissions?")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    Toast.makeText(this, "Media features will be limited without permissions", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }
}