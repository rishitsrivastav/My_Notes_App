package com.example.test;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> implements Filterable, MediaAdapter.MediaListener {
    private static final String TAG = "NoteAdapter";
    
    private List<Note> notes;
    private List<Note> allNotes;
    private final NoteListener noteListener;
    private int lastPosition = -1;
    private Context context;
    
    // Map to store media lists for each note
    private Map<String, List<Media>> mediaMap;
    private MediaRepository mediaRepository;

    public interface NoteListener {
        void onNoteEdit(Note note, int position);
        void onNoteDelete(Note note, int position);
    }

    public NoteAdapter(Context context, NoteListener noteListener) {
        this.context = context;
        this.notes = new ArrayList<>();
        this.allNotes = new ArrayList<>();
        this.noteListener = noteListener;
        this.mediaMap = new HashMap<>();
        this.mediaRepository = new MediaRepository((android.app.Application) context.getApplicationContext());
        Log.d(TAG, "NoteAdapter initialized");
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, position);
        
        // Apply animation to the items
        setAnimation(holder.itemView, position);
    }
    
    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            // Scale animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(viewToAnimate, "scaleX", 0.8f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(viewToAnimate, "scaleY", 0.8f, 1.0f);
            
            // Alpha animation
            ObjectAnimator alpha = ObjectAnimator.ofFloat(viewToAnimate, "alpha", 0.0f, 1.0f);
            
            // Translation animation
            ObjectAnimator translationY = ObjectAnimator.ofFloat(viewToAnimate, "translationY", 100f, 0f);
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY, alpha, translationY);
            animatorSet.setDuration(300);
            animatorSet.setInterpolator(new OvershootInterpolator(1.0f));
            animatorSet.start();
            
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void setNotes(List<Note> notes) {
        Log.d(TAG, "setNotes: Setting " + (notes != null ? notes.size() : 0) + " notes");
        
        // Clear current list
        this.notes.clear();
        this.allNotes.clear();
        
        // Add all notes from the new list
        if (notes != null && !notes.isEmpty()) {
            this.notes.addAll(notes);
            this.allNotes.addAll(notes);
            
            // Log all notes for debugging
            for (Note note : this.notes) {
                Log.d(TAG, "Note in adapter: " + note.getId() + " - " + note.getContent());
                
                // Load media for each note
                loadMediaForNote(note.getId());
            }
        }
        
        // Reset last position for animations
        lastPosition = -1;
        
        // Notify adapter of changes
        notifyDataSetChanged();
    }
    
    private void loadMediaForNote(String noteId) {
        List<Media> mediaList = mediaRepository.getMediaForNote(noteId);
        if (mediaList != null && !mediaList.isEmpty()) {
            mediaMap.put(noteId, mediaList);
            Log.d(TAG, "Loaded " + mediaList.size() + " media items for note: " + noteId);
        } else {
            mediaMap.remove(noteId);
        }
    }

    public void addNote(Note note) {
        Log.d(TAG, "addNote: Adding note: " + note.getId());
        notes.add(0, note);
        allNotes.add(0, note);
        notifyItemInserted(0);
    }

    public Note getNoteAt(int position) {
        if (position >= 0 && position < notes.size()) {
            return notes.get(position);
        }
        return null;
    }
    
    public void updateNote(Note note, int position) {
        Log.d(TAG, "updateNote: Updating note at position " + position + ": " + note.getId());
        if (position >= 0 && position < notes.size()) {
            notes.set(position, note);
            
            // Also update in allNotes list
            for (int i = 0; i < allNotes.size(); i++) {
                if (allNotes.get(i).getId().equals(note.getId())) {
                    allNotes.set(i, note);
                    break;
                }
            }
            
            // Reload media for this note
            loadMediaForNote(note.getId());
            
            notifyItemChanged(position);
        } else {
            Log.w(TAG, "updateNote: Invalid position: " + position);
        }
    }

    public void removeNote(int position) {
        Log.d(TAG, "removeNote: Removing note at position " + position);
        if (position >= 0 && position < notes.size()) {
            Note removedNote = notes.remove(position);
            
            // Also remove from allNotes list
            for (int i = 0; i < allNotes.size(); i++) {
                if (allNotes.get(i).getId().equals(removedNote.getId())) {
                    allNotes.remove(i);
                    break;
                }
            }
            
            // Remove media for this note
            mediaMap.remove(removedNote.getId());
            
            Log.d(TAG, "removeNote: Successfully removed note: " + removedNote.getId());
            notifyItemRemoved(position);
            // Notify adapter of changes to update positions of subsequent items
            notifyItemRangeChanged(position, notes.size() - position);
        } else {
            Log.w(TAG, "removeNote: Invalid position: " + position);
        }
    }
    
    @Override
    public Filter getFilter() {
        return notesFilter;
    }
    
    private final Filter notesFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Note> filteredList = new ArrayList<>();
            
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(allNotes);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                
                for (Note note : allNotes) {
                    if (note.getContent().toLowerCase().contains(filterPattern)) {
                        filteredList.add(note);
                    }
                }
            }
            
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notes.clear();
            @SuppressWarnings("unchecked")
            List<Note> filteredList = (List<Note>) results.values;
            notes.addAll(filteredList);
            
            // Reset last position for animations
            lastPosition = -1;
            
            notifyDataSetChanged();
        }
    };
    
    @Override
    public void onMediaClick(Media media, int position) {
        // Open media viewer
        Intent intent = new Intent(context, MediaViewerActivity.class);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_URI, media.getUri());
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, media.getType());
        context.startActivity(intent);
    }
    
    @Override
    public void onMediaDelete(Media media, int position) {
        // This is handled in the dialog, not in the note list view
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView noteContent;
        private final TextView noteTimestamp;
        private final ImageButton editButton;
        private final ImageButton deleteButton;
        private final RecyclerView mediaRecyclerView;
        private final MaterialCardView mediaCountBadge;
        private final TextView mediaCountText;
        private MediaAdapter mediaAdapter;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteContent = itemView.findViewById(R.id.noteContent);
            noteTimestamp = itemView.findViewById(R.id.noteTimestamp);
            editButton = itemView.findViewById(R.id.editNoteButton);
            deleteButton = itemView.findViewById(R.id.deleteNoteButton);
            mediaRecyclerView = itemView.findViewById(R.id.noteMediaRecyclerView);
            mediaCountBadge = itemView.findViewById(R.id.mediaCountBadge);
            mediaCountText = itemView.findViewById(R.id.mediaCountText);
            
            // Setup media RecyclerView
            mediaAdapter = new MediaAdapter(context, NoteAdapter.this);
            mediaRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            mediaRecyclerView.setAdapter(mediaAdapter);
        }

        public void bind(final Note note, final int position) {
            noteContent.setText(note.getContent());
            noteTimestamp.setText(note.getFormattedDate());
            
            // Setup media
            List<Media> mediaList = mediaMap.get(note.getId());
            if (mediaList != null && !mediaList.isEmpty()) {
                // Show media preview (up to 3 items)
                int previewCount = Math.min(mediaList.size(), 3);
                List<Media> previewList = mediaList.subList(0, previewCount);
                
                mediaAdapter.setMediaList(previewList);
                mediaRecyclerView.setVisibility(View.VISIBLE);
                
                // Show media count badge if there are more than 3 items
                if (mediaList.size() > 3) {
                    mediaCountBadge.setVisibility(View.VISIBLE);
                    mediaCountText.setText(mediaList.size() + " attachments");
                } else {
                    mediaCountBadge.setVisibility(View.GONE);
                }
            } else {
                mediaRecyclerView.setVisibility(View.GONE);
                mediaCountBadge.setVisibility(View.GONE);
            }

            // Add click animation to buttons
            editButton.setOnClickListener(v -> {
                animateButtonClick(v);
                if (noteListener != null) {
                    noteListener.onNoteEdit(note, position);
                }
            });

            deleteButton.setOnClickListener(v -> {
                animateButtonClick(v);
                if (noteListener != null) {
                    noteListener.onNoteDelete(note, position);
                }
            });
            
            // Add ripple effect to the whole item
            itemView.setOnClickListener(v -> {
                // You can add item click behavior here if needed
            });
        }
        
        private void animateButtonClick(View view) {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f);
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f);
            
            scaleDownX.setDuration(100);
            scaleDownY.setDuration(100);
            scaleUpX.setDuration(100);
            scaleUpY.setDuration(100);
            
            AnimatorSet scaleDown = new AnimatorSet();
            scaleDown.play(scaleDownX).with(scaleDownY);
            scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
            
            AnimatorSet scaleUp = new AnimatorSet();
            scaleUp.play(scaleUpX).with(scaleUpY);
            scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(scaleDown).before(scaleUp);
            animatorSet.start();
        }
    }
}