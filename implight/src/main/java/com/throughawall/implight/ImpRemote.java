package com.throughawall.implight;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.throughawall.colorpicker.ColorChangeListener;
import com.throughawall.colorpicker.ColorPickerFragment;
import com.throughawall.colorpicker.ColorTask;
import com.throughawall.colorpicker.GetColorTask;
import com.throughawall.colorpicker.SetColorTask;

import java.io.UnsupportedEncodingException;

public class ImpRemote extends Activity implements ColorChangeListener {
    private static final String TAG = "ImpRemote";

    private ColorPickerFragment mColorPicker;
    private ColorTask mSetColorTask = null;
    private boolean mColorSet = false;
    private Integer mNextColor;
    private String mImpId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imp_remote);

        mColorPicker = (ColorPickerFragment)
                getFragmentManager().findFragmentById(R.id.color_picker_fragment);

        mColorPicker.setColorChangeListener(this);

        mImpId = getResources().getString(R.string.imp_id);
    }

    @Override
    protected void onStart(){
        super.onStart();

        mColorSet = false;

        (new GetColorTask(mImpId, this){
            protected void onPostExecute(ColorResponse result){
                if(!mColorSet && result.getStatus().equals("connected")){
                    mColorPicker.setColor(result.getColor());
                }
            }
        }).execute();
    }

    @Override
    public void onColorChanged(int color) {
        mColorSet = true;
        mNextColor = color;
        setNextColor();
    }

    private void setNextColor(){
        if(mNextColor != null && (mSetColorTask == null || mSetColorTask.getStatus() == AsyncTask.Status.FINISHED)){
            try {
                int nextColor = mNextColor;
                mNextColor = null;
                mSetColorTask = new SetColorTask(mImpId, nextColor, this) {
                    @Override
                    protected void onPostExecute(ColorResponse result) {
                        super.onPostExecute(result);
                        mSetColorTask = null;
                        setNextColor();
                    }
                };
                mSetColorTask.execute();
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
