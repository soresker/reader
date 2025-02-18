package com.example.imagereader;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCaptureException;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CameraPreviewActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView guideText;
    private TextView statusText;
    private GraphicOverlay graphicOverlay;
    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private int detectionCount = 0;
    private static final int REQUIRED_DETECTIONS = 10;
    private View documentGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        previewView = findViewById(R.id.previewView);
        guideText = findViewById(R.id.guideText);
        statusText = findViewById(R.id.statusText);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        documentGuide = findViewById(R.id.documentGuide);

        // Kamera çekme butonu
        FloatingActionButton captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> {
            // Belge tespit edildiğinde butonu yeşil yap
            if (documentGuide.getBackground().getConstantState().equals(
                getResources().getDrawable(R.drawable.document_guide_border_success).getConstantState())) {
                takePicture();
            } else {
                Toast.makeText(this, "Lütfen belgeyi çerçeve içine alın", Toast.LENGTH_SHORT).show();
            }
        });

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e("CAMERA", "Kamera başlatma hatası", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // ImageCapture'ı başlangıçta oluştur
        imageCapture = new ImageCapture.Builder()
            .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build();

        imageAnalysis = new ImageAnalysis.Builder()
            .setTargetResolution(new Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), 
            new TextReaderAnalyzer(new TextReaderAnalyzer.TextReaderListener() {
                @Override
                public void onTextFound(String text, Rect bounds) {
                    graphicOverlay.clear();
                    graphicOverlay.add(new TextGraphic(graphicOverlay, text, bounds));
                    updateGuideText(text);
                }

                @Override
                public void onDocumentDetected(boolean isValid) {
                    if (isValid) {
                        guideText.setText("✅ Belge tespit edildi - Fotoğraf çekmek için dokunun");
                        guideText.setTextColor(getColor(R.color.success_green));
                    } else {
                        guideText.setText("Lütfen belgeyi çerçeve içine alın");
                        guideText.setTextColor(getColor(R.color.text_secondary));
                    }
                }
            }));

        // ImageCapture'ı da bağla
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
    }

    private void updateGuideText(String text) {
        if (text.contains("TÜRKİYE") || text.contains("REPUBLIC")) {
            statusText.setText("Kimlik Kartı Tespit Edildi");
            statusText.setVisibility(View.VISIBLE);
            guideText.setText("Fotoğraf çekmek için butona dokunun");
            documentGuide.setBackgroundResource(R.drawable.document_guide_border_success);
        } else if (text.contains("SÜRÜCÜ") || text.contains("DRIVING")) {
            statusText.setText("Ehliyet Tespit Edildi");
            statusText.setVisibility(View.VISIBLE);
            guideText.setText("Fotoğraf çekmek için butona dokunun");
            documentGuide.setBackgroundResource(R.drawable.document_guide_border_success);
        } else {
            statusText.setVisibility(View.GONE);
            guideText.setText("Lütfen belgeyi çerçeve içine alın");
            documentGuide.setBackgroundResource(R.drawable.document_guide_border);
        }
    }

    private void startAutoCapture() {
        detectionCount++;
        if (detectionCount >= REQUIRED_DETECTIONS) {
            detectionCount = 0;
            takePicture();
        }
    }

    private void takePicture() {
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera hazır değil", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dosya oluştur
        final File photoFile;
        try {
            photoFile = ((MainActivity)MainActivity.context).createImageFile();
        } catch (IOException ex) {
            Log.e("CAMERA_ERROR", "Dosya oluşturma hatası: " + ex.getMessage());
            Toast.makeText(this, "Dosya oluşturulamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageCapture.OutputFileOptions outputFileOptions = 
            new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Fotoğraf çekilirken UI'ı güncelle
        guideText.setText("Fotoğraf çekiliyor...");
        previewView.setEnabled(false);  // Çift tıklamayı önle

        imageCapture.takePicture(outputFileOptions, 
            ContextCompat.getMainExecutor(this),
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                    String photoPath = photoFile.getAbsolutePath();
                    Log.d("CAMERA", "Fotoğraf kaydedildi: " + photoPath);
                    
                    // ResultActivity'yi başlat
                    Intent intent = new Intent(CameraPreviewActivity.this, ResultActivity.class);
                    intent.putExtra("photo_path", photoPath);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("CAMERA", "Fotoğraf çekme hatası", exception);
                    Toast.makeText(CameraPreviewActivity.this, 
                        "Fotoğraf çekilemedi: " + exception.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    previewView.setEnabled(true);
                    guideText.setText("Lütfen tekrar deneyin");
                }
            });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
} 