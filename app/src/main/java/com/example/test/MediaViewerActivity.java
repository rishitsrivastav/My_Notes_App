package com.example.test;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;

public class MediaViewerActivity extends AppCompatActivity {
    private static final String TAG = "MediaViewerActivity";
    
    public static final String EXTRA_MEDIA_URI = "media_uri";
    public static final String EXTRA_MEDIA_TYPE = "media_type";
    
    private ImageView fullscreenImageView;
    private VideoView fullscreenVideoView;
    private ImageButton closeButton;
    private ImageButton shareButton;
    private ProgressBar loadingProgressBar;
    
    private String mediaUri;
    private int mediaType;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make the activity fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_media_viewer);
        
        // Initialize views
        fullscreenImageView = findViewById(R.id.fullscreenImageView);
        fullscreenVideoView = findViewById(R.id.fullscreenVideoView);
        closeButton = findViewById(R.id.closeButton);
        shareButton = findViewById(R.id.shareButton);
        loadingProgressBar = findViewById(R.id.mediaLoadingProgressBar);
        
        // Get media info from intent
        Intent intent = getIntent();
        if (intent != null) {
            mediaUri = intent.getStringExtra(EXTRA_MEDIA_URI);
            mediaType = intent.getIntExtra(EXTRA_MEDIA_TYPE, Media.TYPE_IMAGE);
            
            if (mediaUri != null) {
                loadMedia();
            } else {
                finish();
            }
        } else {
            finish();
        }
        
        // Set click listeners
        closeButton.setOnClickListener(v -> finish());
        
        shareButton.setOnClickListener(v -> shareMedia());
    }
    
    private void loadMedia() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        
        File mediaFile = new File(mediaUri);
        if (!mediaFile.exists()) {
            Log.e(TAG, "Media file does not exist: " + mediaUri);
            finish();
            return;
        }
        
        Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                mediaFile);
        
        if (mediaType == Media.TYPE_IMAGE) {
            // Load image
            fullscreenVideoView.setVisibility(View.GONE);
            fullscreenImageView.setVisibility(View.VISIBLE);
            
            Glide.with(this)
                    .load(contentUri)
                    .fitCenter()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            loadingProgressBar.setVisibility(View.GONE);
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            loadingProgressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(fullscreenImageView);
            
        } else if (mediaType == Media.TYPE_VIDEO) {
            // Load video
            fullscreenImageView.setVisibility(View.GONE);
            fullscreenVideoView.setVisibility(View.VISIBLE);
            
            fullscreenVideoView.setVideoURI(contentUri);
            fullscreenVideoView.setOnPreparedListener(mp -> {
                loadingProgressBar.setVisibility(View.GONE);
                mp.setLooping(true);
                fullscreenVideoView.start();
            });
            
            fullscreenVideoView.setOnErrorListener((mp, what, extra) -> {
                loadingProgressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error playing video: " + what + ", " + extra);
                return false;
            });
            
            fullscreenVideoView.setOnClickListener(v -> {
                if (fullscreenVideoView.isPlaying()) {
                    fullscreenVideoView.pause();
                } else {
                    fullscreenVideoView.start();
                }
            });
        }
    }
    
    private void shareMedia() {
        try {
            File mediaFile = new File(mediaUri);
            if (!mediaFile.exists()) {
                return;
            }
            
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    mediaFile);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            if (mediaType == Media.TYPE_IMAGE) {
                shareIntent.setType("image/*");
            } else {
                shareIntent.setType("video/*");
            }
            
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing media", e);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (fullscreenVideoView.isPlaying()) {
            fullscreenVideoView.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fullscreenVideoView != null) {
            fullscreenVideoView.stopPlayback();
        }
    }
}