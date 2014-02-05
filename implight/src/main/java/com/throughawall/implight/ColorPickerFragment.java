package com.throughawall.implight;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2/2/14.
 */
public class ColorPickerFragment extends Fragment implements ValueChangeListener, ColorChangeListener {
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
        colorChanged();
    }

    @Override
    public void onColorChanged(int color) {
        colorChanged();
    }

    public void colorChanged() {
        int color = getColor();

        for (ColorChangeListener listener : mListeners) {
            listener.onColorChanged(color);
        }
    }

    public int getColor() {
        return 0xFF000000 | (int) (mWheelView.getColor() * mBarView.getValue());
    }
}
