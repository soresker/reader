package com.example.imagereader;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.app.Activity;
import android.view.View;

public class TextGraphic extends GraphicOverlay.Graphic {
    private final String text;
    private final Rect bounds;
    private final Paint textPaint;
    private final Paint rectPaint;
    private final Paint highlightPaint;

    public TextGraphic(GraphicOverlay overlay, String text, Rect bounds) {
        super(overlay);
        this.text = text;
        this.bounds = bounds;

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36.0f);
        textPaint.setAntiAlias(true);

        rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4.0f);

        highlightPaint = new Paint();
        highlightPaint.setColor(Color.argb(50, 0, 255, 0));
        highlightPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        if (bounds == null) {
            return;
        }

        // Sınırlayıcı kutuyu çiz
        Rect scaledBounds = scaleRect(bounds);
        
        // Sadece çerçeve içindeki metinleri göster
        View documentGuide = ((Activity)overlay.getContext())
            .findViewById(R.id.documentGuide);
        Rect guideRect = new Rect();
        documentGuide.getGlobalVisibleRect(guideRect);

        if (isRectInside(scaledBounds, guideRect)) {
            // Önce yarı saydam yeşil arka planı çiz
            canvas.drawRect(scaledBounds, highlightPaint);
            
            // Sonra yeşil çerçeveyi çiz
            canvas.drawRect(scaledBounds, rectPaint);

            // Metni çiz
            float textWidth = textPaint.measureText(text);
            float x = scaledBounds.left;
            float y = scaledBounds.bottom + 40;  // Metnin altına biraz boşluk bırak

            // Metin arka planı
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.argb(180, 0, 0, 0));
            float padding = 8;
            canvas.drawRect(
                x - padding,
                y - 36 - padding,  // textSize + padding
                x + textWidth + padding,
                y + padding,
                bgPaint
            );

            // Metni çiz
            canvas.drawText(text, x, y, textPaint);
        }
    }

    private boolean isRectInside(Rect inner, Rect outer) {
        // Biraz daha esnek kontrol (tamamen içinde olması gerekmiyor)
        int centerX = inner.centerX();
        int centerY = inner.centerY();
        return outer.contains(centerX, centerY);
    }
} 