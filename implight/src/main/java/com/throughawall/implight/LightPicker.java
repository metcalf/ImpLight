package com.throughawall.implight;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by andrew on 2/4/14.
 */
public class LightPicker extends Activity {
    public static String ACTION_LIGHT_PICKER = "com.throughawall.implight.action.LIGHT_PICKER";

    public static String EXTRA_COLOR = "com.throughawall.implight.action.COLOR";
    public static String EXTRA_TIME = "com.throughawall.implight.action.TIME";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imp_remote);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ColorPickerFragment())
                    .commit();
        }
    }
}
