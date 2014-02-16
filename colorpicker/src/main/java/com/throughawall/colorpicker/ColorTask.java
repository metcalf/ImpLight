package com.throughawall.colorpicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by andrew on 2/12/14.
 */


public class ColorTask extends AsyncTask<ColorRequest, Void, ColorTask.ColorResponse> {
    private static final String TAG = "ImpRemote";
    private final int CONNECTION_TIMEOUT = 4000;
    private final int SOCKET_TIMEOUT = 7000;

    protected Exception mError = null;
    private Context mContext;

    public ColorTask(Context context) {
        super();
        mContext = context;
    }

    @Override
    protected ColorResponse doInBackground(ColorRequest... colorRequests) {
        HttpParams httpParams = new BasicHttpParams();
        HttpResponse response = null;
        HttpRequestBase request;

        HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);

        HttpClient client = new DefaultHttpClient(httpParams);

        try {
            request = colorRequests[0].getRequest();
            request.addHeader("Accept", "application/json");

            response = client.execute(request);

            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                Log.d(TAG, "Response: " + out.toString());
                return new ColorResponse(out.toString());
            } else {
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            mError = e;
            return null;
        } catch (IOException e) {
            mError = e;
            return null;
        } catch (JSONException e){
            mError = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(ColorResponse result) {
        if (mError != null) {
            (new AlertDialog.Builder(mContext))
                    .setTitle("Error setting color")
                    .setMessage(mError.toString())
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    })
                    .create()
                    .show();

        }
    }

    public static class ColorResponse {
        private String mStatus;
        private int mColor;

        public ColorResponse(String response) throws JSONException {
            JSONObject jsonObject = new JSONObject(response);

            mStatus = jsonObject.getString("status");

            JSONObject levelsObj = jsonObject.getJSONObject("levels");
            mColor = Color.rgb(levelsObj.getInt("r"), levelsObj.getInt("g"), levelsObj.getInt("b"));
        }

        public String getStatus() {
            return mStatus;
        }

        public int getColor() {
            return mColor;
        }
    }
}
