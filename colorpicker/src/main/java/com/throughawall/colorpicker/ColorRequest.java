package com.throughawall.colorpicker;

import org.apache.http.client.methods.HttpRequestBase;

/**
 * Created by andrew on 2/12/14.
 */
public abstract class ColorRequest {
    protected static final String IMP_FORMAT = "https://agent.electricimp.com/%s/light";

    public abstract HttpRequestBase getRequest();
}
