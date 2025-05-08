package com.example.test;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {
    private static final String TAG = "MediaAdapter";
    
    private List<Media> mediaList;
    private Context context;
    private MediaListener mediaListener;
    
    public interface MediaListener {
        void onMediaClick(Media media, int position);
        void onMediaDelete(Media media, int position);
    }
    
    public MediaAdapter(Context context, MediaListener mediaListener) {
        this.context = context;
        this.mediaList = new ArrayList<>();
        this.mediaListener = mediaListener;
    }
    
    public void setMediaListener(MediaListener mediaListener) {
        this.mediaListener = mediaListener;
    }
    
    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        Media media = mediaList.get(position);
        holder.bind(media, position);
    }
    
    @Override
    public int getItemCount() {
        return mediaList.size();
    }
    
    public void setMediaList(List<Media> mediaList) {
        this.mediaList.clear();
        if (mediaList != null) {
            this.mediaList.addAll(mediaList);
        }
        notifyDataSetChanged();
    }
    
    public void addMedia(Media media) {
        mediaList.add(media);
        notifyItemInserted(mediaList.size() - 1);
    }
    
    public void removeMedia(int position) {
        if (position >= 0 && position < mediaList.size()) {
            mediaList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mediaList.size() - position);
        }
    }
    
    class MediaViewHolder extends RecyclerView.ViewHolder {
        private ImageView mediaImageView;
        private ImageView videoPlayIcon;
        private ImageButton deleteMediaButton;
        
        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            videoPlayIcon = itemView.findViewById(R.id.videoPlayIcon);
            deleteMediaButton = itemView.findViewById(R.id.deleteMediaButton);
        }
        
        public void bind(final Media media, final int position) {
            try {
                Log.d(TAG, "Binding media at position " + position + ": " + media.toString());
                File mediaFile;
                
                if (media.getType() == Media.TYPE_IMAGE) {
                    // Load image
                    mediaFile = new File(media.getUri());
                    Log.d(TAG, "Loading image from: " + mediaFile.getAbsolutePath());
                    videoPlayIcon.setVisibility(View.GONE);
                } else {
                    // Load video thumbnail
                    if (media.getThumbnailUri() != null) {
                        mediaFile = new File(media.getThumbnailUri());
                        Log.d(TAG, "Loading video thumbnail from: " + mediaFile.getAbsolutePath());
                    } else {
                        mediaFile = new File(media.getUri());
                        Log.d(TAG, "Loading video from: " + mediaFile.getAbsolutePath());
                    }
                    videoPlayIcon.setVisibility(View.VISIBLE);
                }
                
                if (mediaFile.exists()) {
                    Log.d(TAG, "Media file exists, loading with Glide");
                    Uri contentUri = Uri.fromFile(mediaFile);
                    
                    // Use try-catch to handle Glide errors
                    try {
                        Glide.with(context)
                                .load(contentUri)
                                .centerCrop()
                                .into(mediaImageView);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading media with Glide", e);
                        // Load a placeholder or show error indicator
                        mediaImageView.setImageResource(android.R.drawable.ic_menu_report_image);
                    }
                } else {
                    Log.e(TAG, "Media file does not exist: " + mediaFile.getAbsolutePath());
                    // Load a placeholder
                    Glide.with(context)
                            .load(android.R.drawable.ic_menu_gallery)
                            .centerCrop()
                            .into(mediaImageView);
                }
                
                // Set click listeners
                itemView.setOnClickListener(v -> {
                    if (mediaListener != null) {
                        mediaListener.onMediaClick(media, position);
                    }
                });
                
                deleteMediaButton.setOnClickListener(v -> {
                    if (mediaListener != null) {
                        mediaListener.onMediaDelete(media, position);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding media", e);
            }
        }
    }
}