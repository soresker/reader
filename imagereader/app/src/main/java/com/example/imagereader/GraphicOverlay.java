package com.example.imagereader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    private final Matrix transformationMatrix = new Matrix();
    private int imageWidth;
    private int imageHeight;
    private float scaleFactor = 1.0f;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
        postInvalidate();
    }

    public void setImageSourceInfo(int width, int height) {
        synchronized (lock) {
            imageWidth = width;
            imageHeight = height;
            calculateTransformationMatrix();
        }
        postInvalidate();
    }

    private void calculateTransformationMatrix() {
        float viewAspectRatio = (float) getWidth() / getHeight();
        float imageAspectRatio = (float) imageWidth / imageHeight;
        float xScale, yScale;

        if (viewAspectRatio > imageAspectRatio) {
            xScale = (float) getWidth() / imageWidth;
            yScale = xScale;
        } else {
            yScale = (float) getHeight() / imageHeight;
            xScale = yScale;
        }

        scaleFactor = xScale;
        transformationMatrix.reset();
        transformationMatrix.postScale(xScale, yScale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }

    public abstract static class Graphic {
        protected final GraphicOverlay overlay;
        protected final Paint paint;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
            this.paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setAntiAlias(true);
        }

        public abstract void draw(Canvas canvas);

        protected float scaleX(float x) {
            return x * overlay.scaleFactor;
        }

        protected float scaleY(float y) {
            return y * overlay.scaleFactor;
        }

        protected Rect scaleRect(Rect rect) {
            return new Rect(
                (int) scaleX(rect.left),
                (int) scaleY(rect.top),
                (int) scaleX(rect.right),
                (int) scaleY(rect.bottom)
            );
        }
    }
} 