package com.whtstable.master;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * BorderDrawable - Custom drawable that renders individual borders on each side
 * Supports different border styles (thin, medium, thick, double, dashed, dotted)
 */
public class BorderDrawable extends Drawable {
    
    private CellBorder cellBorder;
    private Paint topPaint, bottomPaint, leftPaint, rightPaint;
    private Paint fillPaint;
    private float density;
    
    public BorderDrawable(CellBorder border, float density) {
        this.cellBorder = border;
        this.density = density;
        
        fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        
        topPaint = createPaintForStyle(border.top, border.topColor);
        bottomPaint = createPaintForStyle(border.bottom, border.bottomColor);
        leftPaint = createPaintForStyle(border.left, border.leftColor);
        rightPaint = createPaintForStyle(border.right, border.rightColor);
    }
    
    private Paint createPaintForStyle(int style, String color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        
        try {
            paint.setColor(Color.parseColor(color));
        } catch (IllegalArgumentException e) {
            paint.setColor(Color.BLACK);
        }
        
        float strokeWidth = dpToPx(CellBorder.getThicknessDp(style));
        paint.setStrokeWidth(strokeWidth);
        
        // Set path effect for dashed/dotted
        switch (style) {
            case CellBorder.STYLE_DASHED:
                paint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(2)}, 0));
                break;
            case CellBorder.STYLE_DOTTED:
                paint.setPathEffect(new DashPathEffect(new float[]{dpToPx(1), dpToPx(2)}, 0));
                break;
            default:
                paint.setPathEffect(null);
                break;
        }
        
        return paint;
    }
    
    private float dpToPx(int dp) {
        return dp * density;
    }
    
    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        
        // Draw fill background
        canvas.drawRect(bounds, fillPaint);
        
        // Draw borders
        float left = bounds.left;
        float top = bounds.top;
        float right = bounds.right;
        float bottom = bounds.bottom;
        
        // Top border
        if (cellBorder.top != CellBorder.STYLE_NONE) {
            float strokeWidth = topPaint.getStrokeWidth();
            float y = top + strokeWidth / 2;
            
            if (cellBorder.top == CellBorder.STYLE_DOUBLE) {
                // Draw two lines for double border
                canvas.drawLine(left, top + dpToPx(1), right, top + dpToPx(1), topPaint);
                canvas.drawLine(left, top + dpToPx(3), right, top + dpToPx(3), topPaint);
            } else {
                canvas.drawLine(left, y, right, y, topPaint);
            }
        }
        
        // Bottom border
        if (cellBorder.bottom != CellBorder.STYLE_NONE) {
            float strokeWidth = bottomPaint.getStrokeWidth();
            float y = bottom - strokeWidth / 2;
            
            if (cellBorder.bottom == CellBorder.STYLE_DOUBLE) {
                // Draw two lines for double border
                canvas.drawLine(left, bottom - dpToPx(1), right, bottom - dpToPx(1), bottomPaint);
                canvas.drawLine(left, bottom - dpToPx(3), right, bottom - dpToPx(3), bottomPaint);
            } else {
                canvas.drawLine(left, y, right, y, bottomPaint);
            }
        }
        
        // Left border
        if (cellBorder.left != CellBorder.STYLE_NONE) {
            float strokeWidth = leftPaint.getStrokeWidth();
            float x = left + strokeWidth / 2;
            
            if (cellBorder.left == CellBorder.STYLE_DOUBLE) {
                canvas.drawLine(left + dpToPx(1), top, left + dpToPx(1), bottom, leftPaint);
                canvas.drawLine(left + dpToPx(3), top, left + dpToPx(3), bottom, leftPaint);
            } else {
                canvas.drawLine(x, top, x, bottom, leftPaint);
            }
        }
        
        // Right border
        if (cellBorder.right != CellBorder.STYLE_NONE) {
            float strokeWidth = rightPaint.getStrokeWidth();
            float x = right - strokeWidth / 2;
            
            if (cellBorder.right == CellBorder.STYLE_DOUBLE) {
                canvas.drawLine(right - dpToPx(1), top, right - dpToPx(1), bottom, rightPaint);
                canvas.drawLine(right - dpToPx(3), top, right - dpToPx(3), bottom, rightPaint);
            } else {
                canvas.drawLine(x, top, x, bottom, rightPaint);
            }
        }
        
        // If no custom borders, draw default thin border
        if (!cellBorder.hasAnyBorder()) {
            Paint defaultPaint = new Paint();
            defaultPaint.setColor(Color.parseColor("#BDBDBD"));
            defaultPaint.setStyle(Paint.Style.STROKE);
            defaultPaint.setStrokeWidth(dpToPx(1));
            canvas.drawRect(bounds, defaultPaint);
        }
    }
    
    @Override
    public void setAlpha(int alpha) {
        topPaint.setAlpha(alpha);
        bottomPaint.setAlpha(alpha);
        leftPaint.setAlpha(alpha);
        rightPaint.setAlpha(alpha);
        fillPaint.setAlpha(alpha);
    }
    
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        topPaint.setColorFilter(colorFilter);
        bottomPaint.setColorFilter(colorFilter);
        leftPaint.setColorFilter(colorFilter);
        rightPaint.setColorFilter(colorFilter);
    }
    
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
    
    public void updateBorder(CellBorder border) {
        this.cellBorder = border;
        topPaint = createPaintForStyle(border.top, border.topColor);
        bottomPaint = createPaintForStyle(border.bottom, border.bottomColor);
        leftPaint = createPaintForStyle(border.left, border.leftColor);
        rightPaint = createPaintForStyle(border.right, border.rightColor);
        invalidateSelf();
    }
}
