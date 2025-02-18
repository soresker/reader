package com.example.imagereader;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity {
    public static Context context;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        FloatingActionButton cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> checkPermissions());
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            // Yüksek çözünürlük için
            intent.putExtra(MediaStore.EXTRA_OUTPUT, createImageUri());
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024*1024*20); // 20MB
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // En yüksek kalite
            
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("CAMERA_ERROR", "Kamera açılırken hata: ", e);
            Toast.makeText(this, "Kamera açılamadı: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private Uri createImageUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider",
                image);
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
            if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
                if (currentPhotoPath == null) {
                    Log.e("CAMERA_ERROR", "Fotoğraf yolu bulunamadı");
                    Toast.makeText(this, "Fotoğraf yolu bulunamadı", Toast.LENGTH_LONG).show();
                    return;
                }

                File photoFile = new File(currentPhotoPath);
                if (!photoFile.exists()) {
                    Log.e("CAMERA_ERROR", "Fotoğraf dosyası bulunamadı: " + currentPhotoPath);
                    Toast.makeText(this, "Fotoğraf dosyası bulunamadı", Toast.LENGTH_LONG).show();
                    return;
                }

                // Fotoğraf dosyasının yolunu ResultActivity'ye gönder
                Intent intent = new Intent(this, ResultActivity.class);
                intent.putExtra("photo_path", currentPhotoPath);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e("CAMERA_ERROR", "Fotoğraf işlenirken hata: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "Fotoğraf işlenirken hata oluştu: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String currentPhotoPath;

    @Override
    public void onBackPressed() {
        // Uygulamadan çıkmak istediğinden emin misin diye sor
        new AlertDialog.Builder(this)
            .setTitle("Çıkış")
            .setMessage("Uygulamadan çıkmak istiyor musunuz?")
            .setPositiveButton("Evet", (dialog, which) -> {
                finish();
            })
            .setNegativeButton("Hayır", null)
            .show();
    }

    public File createImageFile() throws IOException {
        // Benzersiz bir dosya adı oluştur
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        // Geçici dosya oluştur
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );

        // Dosya yolunu kaydet
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }
}