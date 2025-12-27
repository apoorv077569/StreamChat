package com.example.streamchat.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.streamchat.databinding.ActivityImageViewerBinding;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";
    private ActivityImageViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        Glide.with(this)
                .load(imageUrl)
                .into(binding.fullImageView);

        binding.fullImageView.setOnClickListener(v->finish());

    }
}