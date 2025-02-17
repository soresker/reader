package com.example.imagereader;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class ResultActivity extends AppCompatActivity {
    private TextView tvSoyadi, tvAdi, tvDogumTarihi, tvDogumYeri, tvVerilisTarihi,
            tvGecerlilikTarihi, tvVerildigiIlce, tvTCKN, tvEhliyetNo;
    private ImageView imageView; // Çekilen fotoğrafı göstermek için

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initializeViews();

        Bitmap photo = getIntent().getParcelableExtra("photo");
        if (photo != null) {
            // Çekilen fotoğrafı göster
            imageView.setImageBitmap(photo);
            processImage(photo);
        } else {
            Toast.makeText(this, "Fotoğraf alınamadı", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageView); // Layout'a ImageView eklemeyi unutmayın
        tvSoyadi = findViewById(R.id.tvSoyadi);
        tvAdi = findViewById(R.id.tvAdi);
        tvDogumTarihi = findViewById(R.id.tvDogumTarihi);
        tvDogumYeri = findViewById(R.id.tvDogumYeri);
        tvVerilisTarihi = findViewById(R.id.tvVerilisTarihi);
        tvGecerlilikTarihi = findViewById(R.id.tvGecerlilikTarihi);
        tvVerildigiIlce = findViewById(R.id.tvVerildigiIlce);
        tvTCKN = findViewById(R.id.tvTCKN);
        tvEhliyetNo = findViewById(R.id.tvEhliyetNo);

        // Başlangıç değerlerini ayarla
        tvSoyadi.setText("Soyadı: Taranıyor...");
        tvAdi.setText("Adı: Taranıyor...");
        tvDogumTarihi.setText("Doğum Tarihi: Taranıyor...");
        tvDogumYeri.setText("Doğum Yeri: Taranıyor...");
        tvVerilisTarihi.setText("Veriliş Tarihi: Taranıyor...");
        tvGecerlilikTarihi.setText("Geçerlilik Tarihi: Taranıyor...");
        tvVerildigiIlce.setText("Verildiği İlçe: Taranıyor...");
        tvTCKN.setText("TCKN: Taranıyor...");
        tvEhliyetNo.setText("Ehliyet No: Taranıyor...");
    }

    private void processImage(Bitmap bitmap) {
        try {
            // Görüntüyü işle
            Bitmap processedBitmap = preprocessImage(bitmap);

            InputImage image = InputImage.fromBitmap(processedBitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        Log.d("OCR_RAW", "Ham metin:\n" + fullText);
                        extractInformationWithRegex(fullText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OCR_ERROR", "OCR işlemi başarısız: ", e);
                        Toast.makeText(this, "Metin okuma başarısız", Toast.LENGTH_LONG).show();
                        setDefaultErrorValues();
                    });
        } catch (Exception e) {
            Log.e("OCR_ERROR", "Görüntü işleme hatası: ", e);
            Toast.makeText(this, "Görüntü işlenirken hata oluştu", Toast.LENGTH_LONG).show();
            setDefaultErrorValues();
        }
    }

    private Bitmap preprocessImage(Bitmap original) {
        try {
            // Görüntüyü büyüt
            Bitmap scaled = Bitmap.createScaledBitmap(original,
                    original.getWidth() * 2,
                    original.getHeight() * 2, true);

            // Görüntüyü döndür (gerekirse)
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            return Bitmap.createBitmap(scaled, 0, 0,
                    scaled.getWidth(), scaled.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e("IMAGE_PROCESS", "Görüntü ön işleme hatası: ", e);
            return original;
        }
    }

    private void extractInformationWithRegex(String text) {
        // Metni temizle
        text = text.replaceAll("\n", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase();

        Log.d("OCR_CLEANED", "Temizlenmiş metin:\n" + text);

        Map<String, String> patterns = new HashMap<>();
        patterns.put("Soyadı", "1\\.\\s*([A-ZĞÜŞİÖÇ]+)");
        patterns.put("Adı", "2\\.\\s*([A-ZĞÜŞİÖÇa-zğüşıöç\\s]+)");
        patterns.put("Doğum Tarihi", "3\\.\\s*([\\d]{2}\\.[\\d]{2}\\s*[\\d]{4})");
        patterns.put("Doğum Yeri", "3\\..*?[\\d]{4}\\s+([A-ZĞÜŞİÖÇa-zğüşıöç]+)(?:\\s+DA)?");
        patterns.put("Veriliş Tarihi", "DA\\s+([\\d]{2}\\s+[\\d]{2}\\s+[\\d]{4})");
        patterns.put("Geçerlilik Tarihi", "4[B]?\\.?[\\d\\s]*([\\d]{2}\\.[\\d]{2}\\.[\\d]{4})");
        patterns.put("Verildiği İlçe", "D[C]?\\s+4\\s+([A-ZĞÜŞİÖÇa-zğüşıöç]+)");
        patterns.put("TCKN", "4[D]?\\.?(\\d{11})");
        patterns.put("Ehliyet No", "5\\.\\s*(\\d+)");

        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            try {
                Pattern pattern = Pattern.compile(entry.getValue());
                Matcher matcher = pattern.matcher(text);

                if (matcher.find()) {
                    String value = matcher.group(1).trim();

                    // Tarihleri düzenle
                    if (entry.getKey().contains("Tarihi") && value.contains(" ")) {
                        value = value.replace(" ", ".");
                    }

                    // Metin alanlarından sayıları temizle
                    if (Arrays.asList("Soyadı", "Adı", "Doğum Yeri", "Verildiği İlçe")
                            .contains(entry.getKey())) {
                        value = value.replaceAll("\\d+", "").trim();
                    }

                    // TextView'i güncelle
                    updateTextView(entry.getKey(), value);
                    Log.d("OCR_MATCH", entry.getKey() + ": " + value);
                } else {
                    Log.w("OCR_NOMATCH", entry.getKey() + " için eşleşme bulunamadı");
                }
            } catch (Exception e) {
                Log.e("OCR_REGEX", entry.getKey() + " için hata: " + e.getMessage());
            }
        }
    }

    private void updateTextView(String field, String value) {
        TextView tv = null;
        switch (field) {
            case "Soyadı": tv = tvSoyadi; break;
            case "Adı": tv = tvAdi; break;
            case "Doğum Tarihi": tv = tvDogumTarihi; break;
            case "Doğum Yeri": tv = tvDogumYeri; break;
            case "Veriliş Tarihi": tv = tvVerilisTarihi; break;
            case "Geçerlilik Tarihi": tv = tvGecerlilikTarihi; break;
            case "Verildiği İlçe": tv = tvVerildigiIlce; break;
            case "TCKN": tv = tvTCKN; break;
            case "Ehliyet No": tv = tvEhliyetNo; break;
        }

        if (tv != null) {
            tv.setText("Bulunan " + field + ": " + value);
        }
    }

    private void setDefaultErrorValues() {
        tvSoyadi.setText("Soyadı: Okunamadı");
        tvAdi.setText("Adı: Okunamadı");
        tvDogumTarihi.setText("Doğum Tarihi: Okunamadı");
        tvDogumYeri.setText("Doğum Yeri: Okunamadı");
        tvVerilisTarihi.setText("Veriliş Tarihi: Okunamadı");
        tvGecerlilikTarihi.setText("Geçerlilik Tarihi: Okunamadı");
        tvVerildigiIlce.setText("Verildiği İlçe: Okunamadı");
        tvTCKN.setText("TCKN: Okunamadı");
        tvEhliyetNo.setText("Ehliyet No: Okunamadı");
    }
} 