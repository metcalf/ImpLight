package com.throughawall.colorpicker;

import android.graphics.Color;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2/12/14.
 */
public class SetColorRequest extends ColorRequest {
    private static final String TAG = "ImpRemote";

    private HttpPost mRequest;

    public SetColorRequest(String id, int color, int time) throws UnsupportedEncodingException {
        mRequest = new HttpPost(String.format(IMP_FORMAT, id));


        List<NameValuePair> data = new ArrayList<NameValuePair>(4);

        data.add(new BasicNameValuePair("time", Integer.toString(time)));
        data.add(new BasicNameValuePair("r", Integer.toString(Color.red(color))));
        data.add(new BasicNameValuePair("g", Integer.toString(Color.green(color))));
        data.add(new BasicNameValuePair("b", Integer.toString(Color.blue(color))));

        Log.d(TAG, String.format("Requesting color R:%d G:%d B:%d with dim time %dms",
                Color.red(color), Color.green(color), Color.blue(color), time));

        mRequest.setEntity(new UrlEncodedFormEntity(data, "utf-8"));
    }

    public SetColorRequest(String id, int color) throws UnsupportedEncodingException {
        this(id, color, 0);
    }

    public HttpPost getRequest() {
        return mRequest;
    }
}
