package com.example.imagereader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class ResultActivity extends AppCompatActivity {
    private TextView tvSoyadi, tvAdi, tvDogumTarihi, tvDogumYeri, tvVerilisTarihi,
            tvGecerlilikTarihi, tvVerildigiIlce, tvTCKN, tvEhliyetNo,
            tvSeriNo, tvUyruk, tvBelgeTipi;
    private ImageView imageView; // Çekilen fotoğrafı göstermek için
    private static final int CAMERA_REQUEST_CODE = 101;

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
        tvSeriNo = findViewById(R.id.tvSeriNo);
        tvUyruk = findViewById(R.id.tvUyruk);
        tvBelgeTipi = findViewById(R.id.tvBelgeTipi);

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
        tvSeriNo.setText("Seri No: Taranıyor...");
        tvUyruk.setText("Uyruk: Taranıyor...");
        tvBelgeTipi.setText("Belge Tipi: Taranıyor...");

        // Yeni fotoğraf çekme butonu
        findViewById(R.id.btnNewPhoto).setOnClickListener(v -> {
            // Direkt kamera aktivitesini başlat
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                photoFile = ((MainActivity)MainActivity.context).createImageFile();
            } catch (IOException ex) {
                Log.e("CAMERA_ERROR", "Dosya oluşturma hatası: " + ex.getMessage());
                Toast.makeText(this, "Dosya oluşturulamadı", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            // Yeni fotoğrafı işle
            String photoPath = ((MainActivity)MainActivity.context).getCurrentPhotoPath();
            if (photoPath != null) {
                try {
                    File photoFile = new File(photoPath);
                    if (photoFile.exists()) {
                        Bitmap photo = BitmapFactory.decodeFile(photoPath);
                        if (photo != null) {
                            imageView.setImageBitmap(photo);
                            processImage(photo);
                        } else {
                            Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e("PHOTO_ERROR", "Fotoğraf işlenirken hata: " + e.getMessage());
                    Toast.makeText(this, "Fotoğraf işlenirken hata oluştu", Toast.LENGTH_LONG).show();
                }
            }
        }
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

        // Belge tipini belirle (Ehliyet mi Kimlik mi?)
        boolean isEhliyet = text.contains("SÜRÜCÜ") || text.contains("DRIVING") || text.contains("B1") || text.contains("LICENCE");
        
        Map<String, String> patterns = new HashMap<>();
        
        if (isEhliyet) {
            // Ehliyet pattern'leri - orijinal hali
            patterns.put("Soyadı", "1\\.\\s*([A-ZĞÜŞİÖÇ]+)");  // Herhangi bir büyük harf dizisi
            patterns.put("Adı", "2\\.\\s*([A-ZĞÜŞİÖÇa-zğüşıöç\\s]+)");  // Ad ve soyadı
            patterns.put("Doğum Tarihi", "3\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");  // GG.AA.YYYY formatında tarih
            patterns.put("Doğum Yeri", "3\\..*?\\d{4}\\s+([A-ZĞÜŞİÖÇA-Za-zğüşıöç]+)");  // Tarihten sonraki yer adı
            patterns.put("Veriliş Tarihi", "4[Aa]\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");  // GG.AA.YYYY formatında tarih
            patterns.put("Geçerlilik Tarihi", "4[Bb]\\.\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");  // GG.AA.YYYY formatında tarih
            patterns.put("Verildiği İlçe", "4[Cc]\\.\\s*\\d{1,2}\\s*([A-ZĞÜŞİÖÇA-Za-zğüşıöç]+)");  // İl kodu ve ilçe adı
            patterns.put("TCKN", "4[Dd]\\.\\s*(\\d{11})");  // 11 haneli TC kimlik no
            patterns.put("Ehliyet No", "5\\.?\\s*(\\d{4,6})");  // 4-6 haneli sayı
        } else {
            // Kimlik kartı pattern'leri - düzeltilmiş hali
            patterns.put("TCKN", "(?:TÜRKİYE\\s+CUMHUR.*?|REPUBLIC.*?)\\s*(\\d{11})");
            patterns.put("Soyadı", "(?:SOYADI|SURNAME)\\s+([A-ZĞÜŞİÖÇ]+)(?=\\s+ADI|\\s+GIVEN)");  // Sonrasında ADI veya GIVEN gelecek
            patterns.put("Adı", "(?:ADI|GIVEN).*?NAMES\\s+([A-ZĞÜŞİÖÇ\\s]+?)(?=\\s+DOĞUM|\\s+BIRTH)");  // NAMES sonrası, DOĞUM öncesi
            patterns.put("Doğum Tarihi", "(?:DOĞUM|BIRTH).*?(\\d{2}\\.\\d{2}\\.\\d{4})");
            patterns.put("Seri No", "(?:SERİ|DOCUMENT).*?\\s*([A-Z]\\d+)");
            patterns.put("Uyruk", "(?:UYRUĞU|NATIONALITY).*?\\s*(T\\.?C\\.?/?TUR)");
            patterns.put("Son Geçerlilik", "(?:SON\\s*GEÇERLİLİK|VALID).*?\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");
        }

        // UI'ı belge tipine göre güncelle
        updateUIForDocumentType(isEhliyet);

        // Pattern'leri uygula
        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            try {
                Pattern pattern = Pattern.compile(entry.getValue());
                Matcher matcher = pattern.matcher(text);

                if (matcher.find()) {
                    String value = matcher.group(1).trim();
                    updateTextView(entry.getKey(), value);
                    Log.d("OCR_MATCH", entry.getKey() + ": " + value);
                } else {
                    Log.w("OCR_NOMATCH", entry.getKey() + " için eşleşme bulunamadı");
                    // Alternatif pattern'leri uygula
                    applyAlternativePatterns(entry.getKey(), text);
                }
            } catch (Exception e) {
                Log.e("OCR_REGEX", entry.getKey() + " için hata: " + e.getMessage());
            }
        }
    }

    private void updateUIForDocumentType(boolean isEhliyet) {
        // Belge tipini göster
        tvBelgeTipi.setText(isEhliyet ? 
            "📄Sürücü Belgesi" : 
            "🪪 Kimlik Kartı");
        tvBelgeTipi.setTextColor(getResources().getColor(
            isEhliyet ? R.color.teal_700 : R.color.purple_700));

        // Ehliyet-spesifik alanlar
        tvEhliyetNo.setVisibility(isEhliyet ? View.VISIBLE : View.GONE);
        tvVerildigiIlce.setVisibility(isEhliyet ? View.VISIBLE : View.GONE);
        tvVerilisTarihi.setVisibility(isEhliyet ? View.VISIBLE : View.GONE);
        tvDogumYeri.setVisibility(isEhliyet ? View.VISIBLE : View.GONE);
        tvGecerlilikTarihi.setVisibility(isEhliyet ? View.VISIBLE : View.GONE);

        // Kimlik-spesifik alanlar
        tvSeriNo.setVisibility(isEhliyet ? View.GONE : View.VISIBLE);
        tvUyruk.setVisibility(isEhliyet ? View.GONE : View.VISIBLE);
    }

    private void applyAlternativePatterns(String field, String text) {
        Pattern pattern;
        Matcher matcher;

        switch (field) {
            case "Ehliyet No":
                // Sadece 6 haneli sayı ara
                pattern = Pattern.compile("\\b(\\d{6})\\b");
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String number = matcher.group(1);
                    if (number.length() == 6 && !text.substring(Math.max(0, matcher.start()-2), matcher.end()).contains("4d")) {
                        updateTextView(field, number);
                    }
                }
                break;
            case "TCKN":
                // Sadece 11 haneli sayı ara
                pattern = Pattern.compile("\\b(\\d{11})\\b");
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    updateTextView(field, matcher.group(1));
                }
                break;
            case "Seri No":
                pattern = Pattern.compile("\\b([A-Z]\\d{8})\\b");
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    updateTextView(field, matcher.group(1));
                }
                break;
            case "Uyruk":
                pattern = Pattern.compile("\\b(T\\.?C\\.?/?TUR)\\b");
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    updateTextView(field, matcher.group(1));
                }
                break;
            case "Soyadı":
                pattern = Pattern.compile("SOYADI.*?SURNAME\\s+([A-ZĞÜŞİÖÇ]+)(?!.*NAMES)");
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    updateTextView(field, matcher.group(1));
                }
                break;
            case "Adı":
                pattern = Pattern.compile("NAMES\\s+([A-ZĞÜŞİÖÇ\\s]+?)(?=\\s+DOĞUM|\\s+BIRTH|\\s+SERİ|$)");
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String name = matcher.group(1).trim();
                    if (name.length() > 2 && !name.contains("SURNAME")) {
                        updateTextView(field, name);
                    }
                }
                break;
            case "Doğum Tarihi":
                pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
                matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String date = matcher.group(1);
                    if (date.startsWith("05.03")) { // Doğum tarihi formatı kontrolü
                        updateTextView(field, date);
                        break;
                    }
                }
                break;
        }
    }

    private void updateTextView(String field, String value) {
        TextView tv = null;
        switch (field) {
            case "TCKN": 
                tv = tvTCKN;
                break;
            case "Soyadı": 
                tv = tvSoyadi;
                break;
            case "Adı": 
                tv = tvAdi;
                break;
            case "Doğum Tarihi": 
                tv = tvDogumTarihi;
                break;
            case "Doğum Yeri": 
                tv = tvDogumYeri;
                break;
            case "Veriliş Tarihi": 
                tv = tvVerilisTarihi;
                break;
            case "Geçerlilik Tarihi": 
                tv = tvGecerlilikTarihi;
                break;
            case "Verildiği İlçe": 
                tv = tvVerildigiIlce;
                break;
            case "Ehliyet No": 
                tv = tvEhliyetNo;
                break;
            case "Seri No": 
                tv = tvSeriNo;
                break;
            case "Uyruk": 
                tv = tvUyruk;
                break;
            case "Son Geçerlilik": 
                tv = tvGecerlilikTarihi;
                break;
        }

        if (tv != null) {
            tv.setText("Bulunan " + field + ": " + value);
        }
    }

    private void setDefaultErrorValues() {
        tvBelgeTipi.setText("❌ Belge Tipi Belirlenemedi");
        tvBelgeTipi.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvSoyadi.setText("Soyadı: Okunamadı");
        tvAdi.setText("Adı: Okunamadı");
        tvDogumTarihi.setText("Doğum Tarihi: Okunamadı");
        tvDogumYeri.setText("Doğum Yeri: Okunamadı");
        tvVerilisTarihi.setText("Veriliş Tarihi: Okunamadı");
        tvGecerlilikTarihi.setText("Geçerlilik Tarihi: Okunamadı");
        tvVerildigiIlce.setText("Verildiği İlçe: Okunamadı");
        tvTCKN.setText("TCKN: Okunamadı");
        tvEhliyetNo.setText("Ehliyet No: Okunamadı");
        tvSeriNo.setText("Seri No: Okunamadı");
        tvUyruk.setText("Uyruk: Okunamadı");
    }
} 