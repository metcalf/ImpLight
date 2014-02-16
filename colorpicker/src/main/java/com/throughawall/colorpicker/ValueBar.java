package com.throughawall.colorpicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2/2/14.
 */
public class ValueBar extends SeekBar implements ColorChangeListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ImpRemote";

    private static int mOffColor = 0x40FFFFFF;

    private int mColor;

    private List<ValueChangeListener> mListeners = new ArrayList<ValueChangeListener>();

    public ValueBar(Context context) {
        super(context);
        setOnSeekBarChangeListener(this);
    }

    public ValueBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnSeekBarChangeListener(this);
    }

    public ValueBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnSeekBarChangeListener(this);
    }

    public void setValue(float value){
        setProgress(Math.round(value * 100));
    }

    public float getValue(){
        return getProgress() / 100.0f;
    }

    public void setValueChangeListener(ValueChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeValueChangeListener(ValueChangeListener listener) {
        mListeners.remove(listener);
    }

    public void onColorChanged(int color){
        mColor = color;

        if(getProgress() == 0){
            color = mOffColor;
        } else if (color == 0) {
            getProgressDrawable().clearColorFilter();
            getThumb().clearColorFilter();
            return;
        } else {
            float[] hsv = new float[3];

            Color.colorToHSV(color | 0xFF000000, hsv);
            hsv[2] = 1;
            color = Color.HSVToColor(hsv);
        }

        getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        float value = getValue();
        Log.d(TAG, "Progress changed to " + value);
        for(ValueChangeListener listener : mListeners){
            listener.onValueChanged(value);
        }
        onColorChanged(mColor);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
