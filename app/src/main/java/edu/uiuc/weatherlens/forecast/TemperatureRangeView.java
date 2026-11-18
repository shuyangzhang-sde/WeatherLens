package edu.uiuc.weatherlens.forecast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/** All rows share the week's temperature scale, so lengths and positions are comparable. */
public final class TemperatureRangeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final double start;
    private final double end;

    public TemperatureRangeView(Context context, double low, double high, double weekLow, double weekHigh) {
        super(context);
        double span = Math.max(1, weekHigh - weekLow);
        start = Math.max(0, Math.min(1, (low - weekLow) / span));
        end = Math.max(start, Math.min(1, (high - weekLow) / span));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = Math.min(getHeight(), 6 * getResources().getDisplayMetrics().density);
        float top = (getHeight() - height) / 2;
        paint.setShader(null);
        paint.setColor(Color.rgb(239, 233, 246));
        canvas.drawRoundRect(0, top, getWidth(), top + height, height / 2, height / 2, paint);
        float left = (float) (start * Math.max(0, getWidth() - height));
        float right = Math.max(left + height, (float) (end * getWidth()));
        paint.setShader(new LinearGradient(0, 0, Math.max(1, getWidth()), 0,
                new int[]{Color.rgb(153,187,226), Color.rgb(253,207,125), Color.rgb(252,143,122)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(left, top, right, top + height, height / 2, height / 2, paint);
    }
}
