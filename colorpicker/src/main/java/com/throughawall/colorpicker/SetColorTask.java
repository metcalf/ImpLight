package com.throughawall.colorpicker;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2/16/14.
 */
public class SetColorTask extends ColorTask {
    HttpPost mRequest;

    public SetColorTask(String impId, int color, int time, Context context) throws UnsupportedEncodingException{
        super(context);

        mRequest = new HttpPost(String.format(IMP_FORMAT, impId));

        List<NameValuePair> data = new ArrayList<NameValuePair>(4);

        data.add(new BasicNameValuePair("time", Integer.toString(time)));
        data.add(new BasicNameValuePair("r", Integer.toString(Color.red(color))));
        data.add(new BasicNameValuePair("g", Integer.toString(Color.green(color))));
        data.add(new BasicNameValuePair("b", Integer.toString(Color.blue(color))));

        Log.d(TAG, String.format("Requesting color R:%d G:%d B:%d with dim time %dms",
                Color.red(color), Color.green(color), Color.blue(color), time));

        mRequest.setEntity(new UrlEncodedFormEntity(data, "utf-8"));
    }

    public SetColorTask(String impId, int color, Context context) throws UnsupportedEncodingException{
        this(impId, color, 0, context);
    }

    @Override
    protected HttpRequestBase getRequest() {
        return mRequest;
    }
}
