package com.throughawall.implight;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.throughawall.colorpicker.ColorChangeListener;
import com.throughawall.colorpicker.ColorPickerFragment;
import com.throughawall.colorpicker.ColorTask;
import com.throughawall.colorpicker.GetColorRequest;
import com.throughawall.colorpicker.SetColorRequest;

import java.io.UnsupportedEncodingException;

public class ImpRemote extends Activity implements ColorChangeListener {
    private static final String TAG = "ImpRemote";

    private ColorPickerFragment mColorPicker;
    private ColorTask mSetColorTask = null;
    private boolean mColorSet = false;
    private Integer mNextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imp_remote);

        mColorPicker = (ColorPickerFragment)
                getFragmentManager().findFragmentById(R.id.color_picker_fragment);

        mColorPicker.setColorChangeListener(this);

        ColorTask getTask = new ColorTask(this){
            protected void onPostExecute(ColorResponse result){
                if(!mColorSet && result.getStatus().equals("connected")){
                    mColorPicker.setColor(result.getColor());
                }
            }
        };
        getTask.execute(new GetColorRequest(getResources().getString(R.string.imp_id)));
    }

    @Override
    public void onColorChanged(int color) {
        mColorSet = true;
        mNextColor = color;
        setNextColor();
    }

    private void setNextColor(){
        if(mNextColor != null && (mSetColorTask == null || mSetColorTask.getStatus() == AsyncTask.Status.FINISHED)){
            mSetColorTask = new ColorTask(this) {
                @Override
                protected void onPostExecute(ColorResponse result) {
                    super.onPostExecute(result);
                    mSetColorTask = null;
                    setNextColor();
                }
            };
            try {
                int nextColor = mNextColor;
                mNextColor = null;
                mSetColorTask.execute(new SetColorRequest(getResources().getString(R.string.imp_id), nextColor));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
