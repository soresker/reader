package com.example.imagereader;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class TextReaderAnalyzer implements ImageAnalysis.Analyzer {
    private final TextReaderListener listener;
    private final TextRecognizer recognizer;
    private boolean isProcessing = false;

    public interface TextReaderListener {
        void onTextFound(String text, Rect bounds);
        void onDocumentDetected(boolean isValid);
    }

    public TextReaderAnalyzer(TextReaderListener listener) {
        this.listener = listener;
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            isProcessing = true;
            InputImage image = InputImage.fromMediaImage(mediaImage, 
                imageProxy.getImageInfo().getRotationDegrees());

            recognizer.process(image)
                .addOnSuccessListener(text -> {
                    processText(text);
                    isProcessing = false;
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("TEXT_ANALYSIS", "Text tanıma hatası: " + e.getMessage());
                    isProcessing = false;
                    imageProxy.close();
                });
        } else {
            imageProxy.close();
        }
    }

    private void processText(Text text) {
        if (text.getText().isEmpty()) {
            listener.onDocumentDetected(false);
            return;
        }

        // Belge tespiti için bazı anahtar kelimeleri kontrol et
        String fullText = text.getText().toUpperCase();
        boolean isValidDocument = fullText.contains("TÜRKİYE") || 
                                fullText.contains("REPUBLIC") ||
                                fullText.contains("SÜRÜCÜ") ||
                                fullText.contains("DRIVING");

        listener.onDocumentDetected(isValidDocument);

        // En büyük metin bloğunu bul ve işaretle
        Text.TextBlock largestBlock = null;
        int maxArea = 0;

        for (Text.TextBlock block : text.getTextBlocks()) {
            Rect bounds = block.getBoundingBox();
            if (bounds != null) {
                int area = bounds.width() * bounds.height();
                if (area > maxArea) {
                    maxArea = area;
                    largestBlock = block;
                }
            }
        }

        if (largestBlock != null && largestBlock.getBoundingBox() != null) {
            listener.onTextFound(largestBlock.getText(), largestBlock.getBoundingBox());
        }
    }
} 