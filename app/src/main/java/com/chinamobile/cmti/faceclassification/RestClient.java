package com.chinamobile.cmti.faceclassification;

import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSocketFactory;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

public class RestClient {
    public static final String LOG_TAG = RestClient.class.getSimpleName();
    private static long READ_TIMEOUT = 10000;
    private ApiService apiService;

    public RestClient(String baseURL, Context context) {


        try {
            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .create();
            final OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setConnectTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
            okHttpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
            okHttpClient.setWriteTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

            // loading CAs from an InputStream
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream cert = context.getResources().openRawResource(R.raw.server);
            Certificate ca;
            try {
                ca = cf.generateCertificate(cert);
            } finally {
                cert.close();
            }

            // creating a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // creating a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // creating an SSLSocketFactory that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // this is to ignore the javax.net.ssl.SSLPeerUnverifiedException
            // when the host is not verified by using a self signed certificate
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
            // .setLogLevel(RestAdapter.LogLevel.FULL) to get the library http logs
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(baseURL)
                    .setConverter(new GsonConverter(gson))
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .setClient(new OkClient(okHttpClient))
                    .build();

            apiService = restAdapter.create(ApiService.class);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    protected ApiService getApiService() {
        return apiService;
    }
}
