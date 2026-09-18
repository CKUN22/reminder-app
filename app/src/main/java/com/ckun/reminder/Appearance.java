package com.ckun.reminder;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;

/** Shared, offline appearance. Diffused gradients work on Android 8+, without blurring text. */
final class Appearance {
    private Appearance() {}
    static boolean glass(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getBoolean("glass", false);
    }
    static void setGlass(Context context, boolean enabled) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putBoolean("glass", enabled).apply();
    }
    static GradientDrawable shape(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        boolean surface = color == Color.WHITE || color == 0xffFFFCF7 || color == 0xffF5EBDD || color == 0xffF1F1F1;
        if (glass(context) && surface) {
            shape.setOrientation(GradientDrawable.Orientation.TL_BR);
            shape.setColors(new int[]{0xcfffffff, 0x80ffffff});
            shape.setStroke(Math.max(1, Math.round(context.getResources().getDisplayMetrics().density)), 0xe6ffffff);
        } else shape.setColor(color);
        shape.setCornerRadius(radius * context.getResources().getDisplayMetrics().density);
        return shape;
    }
    static Drawable background(Context context, int simpleColor) {
        return glass(context) ? new FrostedBackground() : new ColorDrawable(simpleColor);
    }
    // Large, smoothly diffused color fields provide the backdrop seen through the glass surfaces.
    static final class FrostedBackground extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            canvas.drawColor(0xffEEF2F4);
            glow(canvas, b.left + b.width() * .05f, b.top + b.height() * .12f, b.height() * .65f, 0xffB8D9CF);
            glow(canvas, b.right, b.top + b.height() * .43f, b.height() * .55f, 0xffC9C3E8);
            glow(canvas, b.left + b.width() * .2f, b.bottom, b.height() * .5f, 0xffF0D9C5);
            paint.setShader(null);
        }
        private void glow(Canvas canvas, float x, float y, float radius, int color) {
            paint.setShader(new RadialGradient(x, y, Math.max(1, radius), color, color & 0x00ffffff, Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}
