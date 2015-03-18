package com.wenku8.app.com.wenku8.app.util;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Created by GuYifan on 2015/3/16.
 */
public class WenkuHttpClient {

    private static AsyncHttpClient client = new AsyncHttpClient();
    static {
        client.setEnableRedirects(false);
    }

    public static void get(String url, AsyncHttpResponseHandler responseHandler) {
        client.get(url, null, responseHandler);
    }

    public static void post(String url, AsyncHttpResponseHandler responseHandler) {
        client.post(url, null, responseHandler);
    }

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

}
