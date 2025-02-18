package com.example.imagereader;

import android.os.Bundle;
import android.view.ScaleGestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.chrisbanes.photoview.PhotoView;
import android.graphics.BitmapFactory;

public class ImageViewerActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        PhotoView photoView = findViewById(R.id.photo_view);
        String imagePath = getIntent().getStringExtra("image_path");
        
        if (imagePath != null) {
            photoView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        }

        // Geri tuşuna basıldığında aktiviteyi kapat
        findViewById(R.id.closeButton).setOnClickListener(v -> finish());
    }
} 