package com.throughawall.colorpicker;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.throughawall.colorpicker.R;

import java.lang.Override;import java.lang.String;import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2/2/14.
 */
public class ColorPickerFragment extends Fragment implements ValueChangeListener, ColorChangeListener {
    private final String TAG = "ImpRemote";

    private List<ColorChangeListener> mListeners = new ArrayList<ColorChangeListener>();

    ColorWheel mWheelView;
    ValueBar mBarView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        float value;

        View rootView = inflater.inflate(R.layout.fragment_color_picker, container, false);

        mWheelView = (ColorWheel) rootView.findViewById(R.id.colorwheel);
        mBarView = (ValueBar) rootView.findViewById(R.id.valuebar);

        if (savedInstanceState != null) {
            value = savedInstanceState.getFloat("value");
            mWheelView.setColor(savedInstanceState.getInt("color"));

        } else {
            value = 1.0f;
        }

        mBarView.setValue(value);
        mWheelView.setValue(value);

        mWheelView.setColorChangeListener(mBarView);
        mWheelView.setColorChangeListener(this);

        mBarView.setValueChangeListener(mWheelView);
        mBarView.setValueChangeListener(this);

        colorChanged();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("color", mWheelView.getColor());
        outState.putFloat("value", mBarView.getValue());
    }

    public void setColorChangeListener(ColorChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeColorChangeListener(ColorChangeListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onValueChanged(float value) {
        Log.d(TAG, "Picker value changed to: " + value);
        colorChanged();
    }

    @Override
    public void onColorChanged(int color) {
        Log.d(TAG, "Picker color changed");
        colorChanged();
    }

    public void colorChanged() {
        int color = getColor();

        for (ColorChangeListener listener : mListeners) {
            listener.onColorChanged(color);
        }
    }

    public int getColor() {
        float[] hsv = new float[3];

        Color.colorToHSV(mWheelView.getColor(), hsv);
        hsv[2] = mBarView.getValue();

        return Color.HSVToColor(hsv);
    }

    public void setColor(int color){
        float[] hsv = new float[3];

        Color.colorToHSV(color, hsv);
        float value = hsv[2];

        hsv[2] = 1.0f;
        int hsColor = Color.HSVToColor(hsv);

        float oldProgress = mBarView.getProgress();

        mWheelView.setColor(hsColor, true);

        mBarView.setValue(value);

        // Hack to make sure the UI gets updated
        if(oldProgress == mBarView.getProgress()){
            mWheelView.setColor(hsColor);
        }
    }
}
