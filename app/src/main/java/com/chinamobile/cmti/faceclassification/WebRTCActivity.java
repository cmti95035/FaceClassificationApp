package com.chinamobile.cmti.faceclassification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebRTCActivity extends AppCompatActivity {
    private WebView webView;
    private static final String TAG = "FaceClassification";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc);

        webView = (WebView)findViewById(R.id.webview);

        startWebRTC();
    }

    private void startWebRTC() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WebViewClient()
        {

            @SuppressLint("NewApi")
            //@Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d("MyPersonalRep", "onPermissionRequest");
                runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
            }

        });
        try{
            Log.d(TAG, "will try to connect to https://ec2-52-53-232-49.us-west-1.compute.amazonaws.com:9001/");
            webView.loadUrl("https://ec2-52-53-232-49.us-west-1.compute.amazonaws.com:9001/");
        }catch(Exception ex)
        {
            Log.d(TAG, "exception: " + ex.getMessage());
        }
    }
}
