package com.throughawall.colorpicker;

import android.content.Context;

import org.apache.http.client.methods.HttpGet;

/**
 * Created by andrew on 2/16/14.
 */
public class GetColorTask extends ColorTask {
    private HttpGet mRequest;

    public GetColorTask(String impId, Context context){
        super(context);

        mRequest = new HttpGet(String.format(IMP_FORMAT, impId));
    }

    public HttpGet getRequest(){
        return mRequest;
    }
}
