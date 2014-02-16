package com.throughawall.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2/1/14.
 */
public class ColorWheel extends View implements ValueChangeListener {
    private static final String TAG = "ImpRemote";

    private static int[] mHColors = {0xFFFF0000, 0xFFFF00FF,
            0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
    private static int[] mLColors = {Color.WHITE, Color.WHITE, Color.BLACK, Color.BLACK};
    private static int mPointerDarkColor = 0xC0000000;
    private static int mPointerLightColor = 0xB0FFFFFF;
    private static int mBorderOffColor = 0x40FFFFFF;

    private static float mMinValue = 0.2f;

    private static int mSampleRadius = 128;

    private static float[] mLPositions = {0, 0.1f, 0.9f, 1};

    private float[] mPointerPosition = {0, -1};
    private float mValue = 1.0f;

    private int mWheelMargin;
    private int mWheelBorder;
    private int mPointerRadius;

    private Paint mWheelPaint;
    private Paint mWheelBorderPaint;
    private Paint mPointerPaint;

    private List<ColorChangeListener> mListeners = new ArrayList<ColorChangeListener>();

    // Hackish technique so I don't have to write my own color calculations
    private Bitmap mSampleBitmap;

    public ColorWheel(Context context) {
        super(context);
        init(null, 0);
    }

    public ColorWheel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ColorWheel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray styles = getContext().obtainStyledAttributes(attrs,
                R.styleable.ColorWheel, defStyle, 0);
        final Resources resources = getContext().getResources();

        mWheelMargin = styles.getDimensionPixelSize(
                R.styleable.ColorWheel_wheel_margin,
                resources.getDimensionPixelSize(R.dimen.color_wheel_wheel_margin));

        mWheelBorder = styles.getDimensionPixelSize(
                R.styleable.ColorWheel_wheel_border,
                resources.getDimensionPixelSize(R.dimen.color_wheel_wheel_border));

        mPointerRadius = styles.getDimensionPixelSize(
                R.styleable.ColorWheel_pointer_radius,
                resources.getDimensionPixelSize(R.dimen.color_wheel_pointer_radius));
        int pointerThickness = styles.getDimensionPixelSize(
                R.styleable.ColorWheel_pointer_thickness,
                resources.getDimensionPixelSize(R.dimen.color_wheel_pointer_thickness));

        mWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mWheelBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWheelBorderPaint.setStyle(Paint.Style.STROKE);
        mWheelBorderPaint.setStrokeWidth(mWheelBorder);

        mPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointerPaint.setStyle(Paint.Style.STROKE);
        mPointerPaint.setStrokeWidth(pointerThickness);
        mPointerPaint.setColor(mPointerDarkColor);

        // Fill sample bitmap
        mSampleBitmap = Bitmap.createBitmap(mSampleRadius * 2, mSampleRadius * 2, Bitmap.Config.ARGB_8888);
        Canvas sampleCanvas = new Canvas(mSampleBitmap);
        Paint samplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        samplePaint.setShader(getWheelShader(mSampleRadius, mSampleRadius, mSampleRadius));
        sampleCanvas.drawPaint(samplePaint);

        // Software rendering is necessary for shader compositing
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setColor(int color){
        setColor(color, false);
    }

    public void setColor(int color, boolean silent){
        if(color == 0){
            mPointerPosition[1] = -1;
        } else {
            float[] hsv = new float[3], currHSV = new float[3];
            int currColor;

            Color.colorToHSV(color, hsv);

            mPointerPosition[0] = (float)-Math.toRadians(hsv[0]);
            mPointerPosition[1] = 0.8f * hsv[1] + 0.1f;

            Color.colorToHSV(getColor(), currHSV);
            Log.d(TAG, String.format("Requested: %d,%.2f,%.2f  Set: %d,%.2f,%.2f",
                    (int)hsv[0], hsv[1], hsv[2], (int)currHSV[0], currHSV[1], currHSV[2]));
        }

        if(!silent){
            colorChanged();
        }
    }

    public int getColor(){
        return calculateColor(mPointerPosition[0], mPointerPosition[1]);
    }

    public void onValueChanged(float value) {
        setValue(value);
    }

    public void setValue(float value) {
        mValue = value;

        float adjustedValue = value * (1 - mMinValue) + mMinValue;
        int c = Math.round(255 * adjustedValue);

        ColorFilter valueFilter = new LightingColorFilter(Color.rgb(c, c, c), 0);
        mWheelPaint.setColorFilter(valueFilter);

        if(value < mMinValue){
            mPointerPaint.setColor(mPointerLightColor);
        } else {
            mPointerPaint.setColor(mPointerDarkColor);
        }

        colorChanged();
    }

    public void setColorChangeListener(ColorChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeColorChangeListener(ColorChangeListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float width, height, cx, cy, dx, dy, radius, touchX, touchY;

        touchX = event.getX();
        touchY = event.getY();

        width = getWidth();
        height = getHeight();

        cx = width / 2;
        cy = height / 2;

        radius = getWheelRadius(width);

        dx = (cx - touchX) / radius;
        dy = (cy - touchY) / radius;

        mPointerPosition[0] = (float) (Math.atan2(dy, dx) + Math.PI);
        mPointerPosition[1] = (float) Math.min(1, Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)));

        colorChanged();

        return true;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dim = Math.min(MeasureSpec.getSize(widthMeasureSpec),
                           MeasureSpec.getSize(heightMeasureSpec));

        setMeasuredDimension(dim, dim);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        float cx, cy, radius;

        cx = (float) width / 2;
        cy = (float) height / 2;

        mWheelPaint.setShader(getWheelShader(cx, cy, getWheelRadius((float) width)));
    }

    @Override
    public void onDraw(Canvas canvas) {
        int pointerColor, borderColor;
        float width, height, cx, cy, radius, pointerCx, pointerCy;

        width = getWidth();
        height = getHeight();

        cx = width / 2;
        cy = height / 2;
        radius = getWheelRadius(width);

        canvas.drawCircle(cx, cy, radius, mWheelPaint);

        if(mPointerPosition[1] >= 0){
            pointerColor = getColor();

            pointerCx = cx + (float) (radius * mPointerPosition[1] * Math.cos(mPointerPosition[0]));
            pointerCy = cy + (float) (radius * mPointerPosition[1] * Math.sin(mPointerPosition[0]));

            if(mValue > 0){
                mWheelBorderPaint.setColor(pointerColor);
            } else {
                mWheelBorderPaint.setColor(mBorderOffColor);
            }
            canvas.drawCircle(cx, cy, radius + (float) mWheelBorder / 2, mWheelBorderPaint);

            canvas.drawCircle(pointerCx, pointerCy, mPointerRadius, mPointerPaint);
        }
    }

    protected void colorChanged() {
        int color = getColor();

        for (ColorChangeListener listener : mListeners) {
            listener.onColorChanged(color);
        }

        invalidate();
    }

    private Shader getWheelShader(float cx, float cy, float radius) {
        Shader sweepShader = new SweepGradient(cx, cy, mHColors, null);
        Shader radialShader = new RadialGradient(cx, cy, radius, mLColors, mLPositions, Shader.TileMode.CLAMP);
        return new ComposeShader(radialShader, sweepShader, PorterDuff.Mode.SCREEN);
    }

    private float getWheelRadius(float width) {
        return (width / 2) - mWheelMargin - mWheelBorder;
    }

    /**
     * Calculate the color using the supplied angle and radius.
     *
     * @param angle  The selected color's angle in radians.
     * @param radius The selected color's radius in units (0-1).
     * @return The ARGB value of the color on the color wheel at the specified
     * angle.
     */
    private int calculateColor(float angle, float radius) {
        int x = mSampleRadius + (int) (mSampleRadius * radius * Math.cos(angle));
        int y = mSampleRadius + (int) (mSampleRadius * radius * Math.sin(angle));

        return mSampleBitmap.getPixel(x, y);
    }
}
