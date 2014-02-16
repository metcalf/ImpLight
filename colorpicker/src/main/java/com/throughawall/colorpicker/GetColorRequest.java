package com.throughawall.colorpicker;

import org.apache.http.client.methods.HttpGet;

/**
 * Created by andrew on 2/12/14.
 */
public class GetColorRequest extends ColorRequest {
    private HttpGet mRequest;


    public GetColorRequest(String id){
        mRequest = new HttpGet(String.format(IMP_FORMAT, id));
    }

    public HttpGet getRequest(){
        return mRequest;
    }
}
