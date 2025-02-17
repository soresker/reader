package com.example.imagereader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> checkCameraPermission());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Kamera ayarları
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024*1024*10); // 10MB

            // Yüksek çözünürlük için
            Bundle bundle = new Bundle();
            bundle.putInt("android.intent.extras.CAMERA_FACING", 0);
            bundle.putInt("android.intent.extras.LENS_FACING_FRONT", 0);
            bundle.putInt("android.intent.extras.CAMERA_MODE", 0);
            intent.putExtras(bundle);

            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("CAMERA_ERROR", "Kamera açılırken hata: ", e);
            Toast.makeText(this, "Kamera açılamadı: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Kamera izni reddedildi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
                Bitmap photo = null;

                // Önce tam boyutlu fotoğrafı almayı dene
                if (data.hasExtra("data")) {
                    photo = (Bitmap) data.getExtras().get("data");
                }

                if (photo != null) {
                    // Fotoğrafı ResultActivity'ye gönder
                    Intent intent = new Intent(this, ResultActivity.class);
                    intent.putExtra("photo", photo);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Fotoğraf alınamadı", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("CAMERA_RESULT_ERROR", "Fotoğraf alınırken hata: ", e);
            Toast.makeText(this, "Fotoğraf işlenirken hata oluştu: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}