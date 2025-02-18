package com.example.imagereader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
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

import java.io.File;
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

        String photoPath = getIntent().getStringExtra("photo_path");
        if (photoPath != null) {
            try {
                File photoFile = new File(photoPath);
                if (photoFile.exists()) {
                    Bitmap photo = BitmapFactory.decodeFile(photoPath);
                    if (photo != null) {
                        // Çekilen fotoğrafı göster
                        imageView.setImageBitmap(photo);
                        processImage(photo);
                    } else {
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    Toast.makeText(this, "Fotoğraf dosyası bulunamadı", Toast.LENGTH_LONG).show();
                    finish();
                }
            } catch (Exception e) {
                Log.e("PHOTO_ERROR", "Fotoğraf yüklenirken hata: " + e.getMessage(), e);
                Toast.makeText(this, "Fotoğraf yüklenirken hata oluştu", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Fotoğraf yolu alınamadı", Toast.LENGTH_LONG).show();
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

        // Yeni fotoğraf çekme butonu
        findViewById(R.id.btnNewPhoto).setOnClickListener(v -> {
            // Ana ekrana dön ve tüm aktiviteleri temizle
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void processImage(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                Log.e("OCR_ERROR", "Bitmap null geldi");
                Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_LONG).show();
                setDefaultErrorValues();
                return;
            }

            Log.d("OCR_INFO", "Orijinal görüntü boyutu: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // Görüntüyü işle
            Bitmap processedBitmap = preprocessImage(bitmap);
            
            if (processedBitmap == null) {
                Log.e("OCR_ERROR", "İşlenmiş bitmap null");
                Toast.makeText(this, "Görüntü işlenemedi", Toast.LENGTH_LONG).show();
                setDefaultErrorValues();
                return;
            }
            
            Log.d("OCR_INFO", "İşlenmiş görüntü boyutu: " + processedBitmap.getWidth() + "x" + processedBitmap.getHeight());
            
            // İşlenmiş görüntüyü göster
            imageView.setImageBitmap(processedBitmap);
            
            // OCR için görüntüyü hazırla
            InputImage image = InputImage.fromBitmap(processedBitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        Log.d("OCR_RAW", "Ham metin:\n" + fullText);
                        
                        if (fullText.isEmpty()) {
                            Log.w("OCR_WARN", "OCR sonucu boş metin döndü");
                            Toast.makeText(this, "Metin okunamadı", Toast.LENGTH_LONG).show();
                            setDefaultErrorValues();
                            return;
                        }
                        
                        // Metin temizleme ve düzenleme
                        fullText = cleanText(fullText);
                        Log.d("OCR_CLEANED", "Temizlenmiş metin:\n" + fullText);
                        
                        extractInformationWithRegex(fullText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OCR_ERROR", "OCR işlemi başarısız: " + e.getMessage(), e);
                        Toast.makeText(this, "Metin okuma başarısız: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        setDefaultErrorValues();
                    });
        } catch (Exception e) {
            Log.e("OCR_ERROR", "Görüntü işleme hatası: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "Görüntü işlenirken hata oluştu: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            setDefaultErrorValues();
        }
    }

    private Bitmap preprocessImage(Bitmap original) {
        try {
            if (original == null || original.isRecycled()) {
                Log.e("IMAGE_PROCESS", "Geçersiz bitmap");
                return null;
            }

            Log.d("IMAGE_PROCESS", "Orijinal boyut: " + original.getWidth() + "x" + original.getHeight());
            
            // Görüntüyü yeniden boyutlandır
            int targetWidth = 2000;
            float ratio = (float) targetWidth / original.getWidth();
            int targetHeight = (int) (original.getHeight() * ratio);
            
            Bitmap scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);
            Log.d("IMAGE_PROCESS", "Yeniden boyutlandırma sonrası: " + scaled.getWidth() + "x" + scaled.getHeight());
            
            // Gri tonlama için ColorMatrix kullan
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            
            // Kontrast ve parlaklık ayarı
            ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                2.0f, 0, 0, 0, -50, // Daha yüksek kontrast
                0, 2.0f, 0, 0, -50,
                0, 0, 2.0f, 0, -50,
                0, 0, 0, 1, 0
            });
            
            colorMatrix.postConcat(contrastMatrix);
            
            Bitmap processed = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), 
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(processed);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            
            canvas.drawBitmap(scaled, 0, 0, paint);
            Log.d("IMAGE_PROCESS", "İşleme tamamlandı");
            
            if (!scaled.isRecycled()) {
                scaled.recycle();
            }
            
            return processed;
        } catch (Exception e) {
            Log.e("IMAGE_PROCESS", "Görüntü ön işleme hatası: " + e.getMessage(), e);
            e.printStackTrace();
            return original;
        }
    }

    private String cleanText(String text) {
        return text.replaceAll("[^a-zA-Z0-9ğüşıöçĞÜŞİÖÇ.\\s]", "") // Özel karakterleri temizle
                   .replaceAll("\\s+", " ") // Fazla boşlukları temizle
                   .trim()
                   .toUpperCase();
    }

    private void extractInformationWithRegex(String text) {
        // Metni temizle
        text = text.replaceAll("\n", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase();

        Log.d("OCR_CLEANED", "Temizlenmiş metin:\n" + text);

        Map<String, String> patterns = new HashMap<>();
        patterns.put("Soyadı", "1\\.\\s*([A-ZĞÜŞİÖÇ]+)");  // Herhangi bir büyük harf dizisi
        patterns.put("Adı", "2\\.\\s*([A-ZĞÜŞİÖÇa-zğüşıöç\\s]+)");  // Ad ve soyadı
        patterns.put("Doğum Tarihi", "3\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");  // GG.AA.YYYY formatında tarih
        patterns.put("Doğum Yeri", "3\\..*?\\d{4}\\s+([A-ZĞÜŞİÖÇA-Za-zğüşıöç]+)");  // Tarihten sonraki yer adı
        patterns.put("Veriliş Tarihi", "4[Aa]\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");  // GG.AA.YYYY formatında tarih
        patterns.put("Geçerlilik Tarihi", "4[Bb]\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");  // GG.AA.YYYY formatında tarih
        patterns.put("Verildiği İlçe", "4[Cc]\\.\\s*\\d{1,2}\\s*([A-ZĞÜŞİÖÇA-Za-zğüşıöç]+)");  // İl kodu ve ilçe adı
        patterns.put("TCKN", "4[Dd]\\.\\s*(\\d{11})");  // 11 haneli TC kimlik no
        patterns.put("Ehliyet No", "5\\.?\\s*(\\d{4,6})");  // 4-6 haneli sayı

        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            try {
                Pattern pattern = Pattern.compile(entry.getValue());
                Matcher matcher = pattern.matcher(text);

                if (matcher.find()) {
                    String value = matcher.group(1).trim();
                    
                    // Ehliyet No için özel kontrol
                    if (entry.getKey().equals("Ehliyet No")) {
                        // 3 rakamdan uzun ve TCKN olmayan sayıları al
                        if (value.length() > 3 && value.length() != 11) {
                            updateTextView(entry.getKey(), value);
                            continue;
                        }
                    }
                    
                    updateTextView(entry.getKey(), value);
                    Log.d("OCR_MATCH", entry.getKey() + ": " + value);
                } else {
                    Log.w("OCR_NOMATCH", entry.getKey() + " için eşleşme bulunamadı");
                    
                    // Ehliyet No için alternatif pattern'ler
                    if (entry.getKey().equals("Ehliyet No")) {
                        // İlk alternatif: 5. ile başlayan herhangi bir sayı
                        pattern = Pattern.compile("5\\.\\s*(\\d+)");
                        matcher = pattern.matcher(text);
                        if (matcher.find()) {
                            String number = matcher.group(1);
                            if (number.length() > 3 && number.length() != 11) {
                                updateTextView(entry.getKey(), number);
                                continue;
                            }
                        }

                        // İkinci alternatif: 6 haneli sayı
                        pattern = Pattern.compile("(\\d{6})");
                        matcher = pattern.matcher(text);
                        while (matcher.find()) {
                            String number = matcher.group(1);
                            // TCKN olmadığından emin ol ve 134229'u bul
                            if (number.length() == 6 && !number.equals(text.substring(matcher.start()-2, matcher.start()+6).contains("4d"))) {
                                updateTextView(entry.getKey(), number);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("OCR_REGEX", entry.getKey() + " için hata: " + e.getMessage());
            }
        }
    }

    private void updateTextView(String field, String value) {
        TextView tv = null;
        switch (field) {
            case "Soyadı": 
                tv = tvSoyadi;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Adı": 
                tv = tvAdi;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Doğum Tarihi": 
                tv = tvDogumTarihi;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Doğum Yeri": 
                tv = tvDogumYeri;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Veriliş Tarihi": 
                tv = tvVerilisTarihi;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Geçerlilik Tarihi": 
                tv = tvGecerlilikTarihi;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Verildiği İlçe": 
                tv = tvVerildigiIlce;
                value = "Bulunan " + field + ": " + value;
                break;
            case "TCKN": 
                tv = tvTCKN;
                value = "Bulunan " + field + ": " + value;
                break;
            case "Ehliyet No": 
                tv = tvEhliyetNo;
                value = "Bulunan " + field + ": " + value;
                break;
        }

        if (tv != null) {
            tv.setText(value);
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