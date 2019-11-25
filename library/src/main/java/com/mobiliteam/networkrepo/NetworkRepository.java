package com.mobiliteam.networkrepo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mobiliteam.networkrepo.gsonadapter.CustomGsonTypeAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

/**
 * Created by admin on 13/04/18.
 */

public class NetworkRepository implements INetworkRepository {

    private static final String NET_PREF = "inetxrepjdojd";
    private static final String NET_PREF_ITEM = "inetxrepjdojd_mitm";
    public static final String ODATA_DATA_NODE_NAME = "value";
    private static final String V3_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";

    private String TAG = NetworkRepository.class.getSimpleName();

    private static NetworkRepository instance;
    private Context context;
    private OkHttpClient okHttpClient;
    private int defaultTimeout = 60; // SECONDS
    private boolean allowRetry = false;
    private OkHttpClient.Builder clientBuilder;
    private SharedPreferences sharedpreferences;
    private TokenResponse tokenResponse = null;
    private IRefreshTokenListener refreshTokenListener;
    private Handler handler;
    private String customNetPrefItem = NET_PREF_ITEM;
    private String dateFormat = V3_FORMAT;
    private String nodeValue = ODATA_DATA_NODE_NAME;
    private boolean booleanToInt = false;
    private boolean dateToUTC = false;
    private boolean addConnectionSpecs = false;
    private boolean addTLSPermissionBelowLollypop = false;


    private NetworkRepository(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        sharedpreferences = context.getSharedPreferences(NET_PREF, Context.MODE_PRIVATE);
    }

    public static synchronized NetworkRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkRepository(context);
        }
        return instance;
    }

    public static synchronized NetworkRepository createInstance(Context context) {
        return new NetworkRepository(context);
    }

    public MediaType getJsonMediaType() {
        return MediaType.parse("application/json; charset=utf-8");
    }

    public MediaType getPngMediaType() {
        return MediaType
                .parse("image/png");
    }

    public MediaType getJpegType() {
        return MediaType
                .parse("image/jpeg");
    }

    private void setCustomStoreName(final String prefStoreName) {
        if (!TextUtils.isEmpty(prefStoreName)) {
            this.customNetPrefItem = prefStoreName;
        } else {
            this.customNetPrefItem = NET_PREF_ITEM;
        }
    }

    public Gson getGson() {
        return getGsonBuilder()
                .create();
    }

    public GsonBuilder getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setDateFormat(dateFormat);
        gsonBuilder.registerTypeAdapter(CustomGsonTypeAdapter.getIntegerType(), CustomGsonTypeAdapter.IntegerTypeAdapter);
        gsonBuilder.registerTypeAdapter(CustomGsonTypeAdapter.getDoubleType(), CustomGsonTypeAdapter.DoubleTypeAdapter);
        if (booleanToInt) {
            gsonBuilder.registerTypeAdapter(Boolean.class, new CustomGsonTypeAdapter.PrimBooleanToIntAdapter());
            gsonBuilder.registerTypeAdapter(boolean.class, new CustomGsonTypeAdapter.PrimBooleanToIntAdapter());
        } else {
            gsonBuilder.registerTypeAdapter(CustomGsonTypeAdapter.getBooleanType(), CustomGsonTypeAdapter.BooleanTypeAdapter);
        }
        if (dateToUTC) {
            gsonBuilder.registerTypeAdapter(Date.class, new CustomGsonTypeAdapter.GsonUTCDateAdapter(dateFormat));
        }
        return gsonBuilder;
    }

    /*
    Setup is required to generate ODATA compatible network client which sets interceptor and
    default timeout and retry policies
     */

    public void setup(final int timeoutInSeconds, final boolean retryRequest) {
        defaultTimeout = timeoutInSeconds;
        allowRetry = retryRequest;

        // TLS changes for Kitkat 4.4
        if(addConnectionSpecs) {
            List<CipherSuite> cipherSuites = new ArrayList<>();
            cipherSuites.addAll(ConnectionSpec.MODERN_TLS.cipherSuites());
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA);
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA);
            ConnectionSpec legacyTls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .cipherSuites(cipherSuites.toArray(new CipherSuite[0]))
                    .build();


            clientBuilder = new OkHttpClient.Builder()
                    .readTimeout(defaultTimeout, TimeUnit.SECONDS)
                    .connectTimeout(defaultTimeout, TimeUnit.SECONDS)
                    .writeTimeout(defaultTimeout, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(allowRetry)
                    .sslSocketFactory(getSocketFactory())
                    .addInterceptor(new OAuth2TokenInterceptor());

        } else {
            clientBuilder = new OkHttpClient.Builder()
                    .readTimeout(defaultTimeout, TimeUnit.SECONDS)
                    .connectTimeout(defaultTimeout, TimeUnit.SECONDS)
                    .writeTimeout(defaultTimeout, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(allowRetry)
                    .addInterceptor(new OAuth2TokenInterceptor());
        }

        okHttpClient = clientBuilder.build();

        checkLoggedIn();
    }


    private SSLSocketFactory getSocketFactory() {
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            X509TrustManager x509Tm = null;
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    x509Tm = (X509TrustManager) tm;
                    break;
                }
            }

            final X509TrustManager finalTm = x509Tm;
            X509TrustManager customTm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return finalTm.getAcceptedIssuers();
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                    finalTm.checkServerTrusted(chain, authType);
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                    finalTm.checkClientTrusted(chain, authType);
                }
            };


            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{customTm}, null);
            return sslContext.getSocketFactory();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setup(final int timeoutInSeconds, final boolean retryRequest, boolean addConnectionSpecs) {
        this.addConnectionSpecs = addConnectionSpecs;
        this.setup(timeoutInSeconds, retryRequest);
    }

    public void setup(final int timeoutInSeconds, final boolean retryRequest, String prefStoreName) {
        this.setCustomStoreName(prefStoreName);
        this.setup(timeoutInSeconds, retryRequest);
    }



    public void setDateFormat(final String dateFormat) {
        this.dateFormat = dateFormat != null ? dateFormat : V3_FORMAT;
    }

    public void setAsRestAPI() {
        this.nodeValue = null;
    }

    public void setRefreshTokenListener(IRefreshTokenListener listener) {
        this.refreshTokenListener = listener;
    }

    public void boolToIntInNetworkCall(final boolean value) {
        this.booleanToInt = value;
    }

    public void setDateToUTC(final boolean value) {
        this.dateToUTC = value;
    }

    /*
    This returns default client with TIMEOUT of 10sec, retry=true and no refresh token interceptors
     */

    public OkHttpClient requestNewClient() {
        return new OkHttpClient.Builder().build();
    }

    private Headers getDefaultHeaders() {
//        Headers.Builder builder = new Headers.Builder();
//        builder.add("Content-Type", "application/json");
//        if (tokenResponse != null) {
//            builder.add("Authorization", tokenResponse.getTokenType() + " " + tokenResponse.getAccessToken());
//        }
//        return builder.build();
        return getDefaultHeaders("application/json");
    }

    private Headers getDefaultHeaders(final String contentType) {
        Headers.Builder builder = new Headers.Builder();
        builder.add("Content-Type", contentType);
        if (tokenResponse != null) {
            builder.add("Authorization", tokenResponse.getTokenType() + " " + tokenResponse.getAccessToken());
        }
        return builder.build();
    }

    /**
     * Login funtion required to connect ODATA ENDPOINT
     *
     * @param url           Login Url (STS url)
     * @param loginParams   Pass all header including username and password
     * @param refreshParams Pass all headers without Refresh Token (refresh_token header)
     * @param loginHeaders  Pass headers if available otherwise null
     * @param listener      This depicts multi threading response
     */

    public void login(final String url, LinkedHashMap<String, String> loginParams, final LinkedHashMap<String, String> refreshParams, LinkedHashMap<String, String> loginHeaders, final IConnectionListener listener) {

        FormBody.Builder builder = new FormBody.Builder();
        Set<String> keys = loginParams.keySet();
        for (String key : keys) {
            builder.add(key, loginParams.get(key));
        }
        RequestBody requestBody = builder.build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (loginHeaders != null && loginHeaders.keySet().size() > 0) {
            Headers.Builder headerBuilder = new Headers.Builder();
            Set<String> headerKeys = loginHeaders.keySet();
            for (String headerKey : headerKeys) {
                headerBuilder.add(headerKey, loginHeaders.get(headerKey));
            }
            requestBuilder.headers(headerBuilder.build());
        }

        final NetworkResponse networkResponse = new NetworkResponse();
        requestNewClient().newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(NetworkHttpMethod.GET, code);
                networkResponse.setSuccess(valid);
                try {
                    if (valid) {
                        tokenResponse = getGson().fromJson(body, TokenResponse.class);
                        tokenResponse.setTokenUrl(url);
                        Set<String> keys = refreshParams.keySet();
                        for (String key : keys) {
                            tokenResponse.getRefreshHeaders().add(new APIHeaderField(key, refreshParams.get(key)));
                        }
                        saveDetails();
                        //listener.onResponse(call, networkResponse);
                        publishSuccess(listener, call, networkResponse);
                    } else {
                        networkResponse.setErrorMsg("" + body);
                        //listener.onFailure(call, networkResponse);
                        publishFailure(listener, call, networkResponse);
                    }
                } catch (Exception e) {
                    networkResponse.setSuccess(false);
                    networkResponse.setErrorMsg("" + e.toString());
                    networkResponse.setResponse("" + body);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    private void publishSuccess(final IConnectionListener listener, final Call call, final NetworkResponse networkResponse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onResponse(call, networkResponse);
            }
        });
    }

    private void publishFailure(final IConnectionListener listener, final Call call, final NetworkResponse networkResponse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onFailure(call, networkResponse);
            }
        });
    }

    private NetworkResponse refreshToken() {
        NetworkResponse networkResponse = new NetworkResponse();
        FormBody.Builder builder = new FormBody.Builder();
        for (APIHeaderField field : tokenResponse.getRefreshHeaders()) {
            if (!field.getKey().equalsIgnoreCase(tokenResponse.refreshTokenkey())) {
                builder.add(field.getKey(), field.getValue());
            }
        }
        if(tokenResponse.getRefreshToken() != null) {
            builder.add(tokenResponse.refreshTokenkey(), tokenResponse.getRefreshToken());
        }
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(tokenResponse.getTokenUrl())
                .post(requestBody)
                .build();
        try {
            Response response = requestNewClient().newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            boolean valid = validate(NetworkHttpMethod.GET, code);
            networkResponse.setSuccess(valid);
            networkResponse.setResponse("" + tokenResponse.getRefreshToken());
            if (valid) {
                TokenResponse refreshTokenResponse = getGson().fromJson(body, TokenResponse.class);
                tokenResponse.setAccessToken(refreshTokenResponse.getAccessToken());
                tokenResponse.setRefreshToken(refreshTokenResponse.getRefreshToken());
                saveDetails();
            }
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    private void checkLoggedIn() {
        if (sharedpreferences.contains(customNetPrefItem)) {
            final String json = sharedpreferences.getString(customNetPrefItem, "");
            if (json.trim().length() > 0) {
                tokenResponse = getGson().fromJson(json, TokenResponse.class);
            }
        }
    }

    private void saveDetails() {
        final String json = getGson().toJson(tokenResponse, TokenResponse.class);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(customNetPrefItem, json);
        editor.commit();
        checkLoggedIn();
    }

    @Override
    public <T> NetworkResponse get(Class<T> modelClass, String url) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            Request request = genRequest(url, NetworkHttpMethod.GET, null);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(NetworkHttpMethod.GET, code));
            networkResponse.setResponse(getGson().fromJson(body, modelClass));
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public NetworkResponse get(String url) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            Request request = genRequest(url, NetworkHttpMethod.GET, null);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(NetworkHttpMethod.GET, code));
            networkResponse.setResponse("" + body);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public <T> void get(final Class<T> modelClass, String url, final IConnectionListener listener) {
        get(modelClass, url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void get(final Class<T> modelClass, String url, final int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    networkResponse.setResponse(getGson().fromJson(body, modelClass));
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public <T> void get(String url, IConnectionListener listener) {
        get(url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void get(String url, int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    networkResponse.setResponse("" + body);
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public <T> NetworkResponse getAll(Class<T> modelClass, String url) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            Request request = genRequest(url, NetworkHttpMethod.GET, null);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(NetworkHttpMethod.GET, code));

            JSONArray jsonArray = null;
            if (nodeValue != null) {
                jsonArray = new JSONObject(body).getJSONArray(nodeValue);
            } else {
                jsonArray = new JSONArray(body);
            }
            List<T> items = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                T item = getGson().fromJson(jsonArray.getJSONObject(i).toString(), modelClass);
                items.add(item);
            }

            networkResponse.setResponse(items);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public <T> NetworkResponse getAll(Type modelType, String url) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            Request request = genRequest(url, NetworkHttpMethod.GET, null);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(NetworkHttpMethod.GET, code));

            JSONArray jsonArray = null;
            if (nodeValue != null) {
                jsonArray = new JSONObject(body).getJSONArray(nodeValue);
            } else {
                jsonArray = new JSONArray(body);
            }
//            List<T> items = new ArrayList<>();
//            for (int i = 0; i < jsonArray.length(); i++) {
//                T item = getGson().fromJson(jsonArray.getJSONObject(i).toString(), modelClass);
//                items.add(item);
//            }

            List<T> items = getGson().fromJson(jsonArray.toString(), modelType);
            networkResponse.setResponse(items);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public <T> void getAll(String url, IConnectionListener listener) {
        getAll(url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void getAll(String url, final int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    networkResponse.setResponse("" + body);
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public <T> void getAll(final Class<T> modelClass, final String url, final IConnectionListener listener) {
        getAll(modelClass, url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void getAll(final Class<T> modelClass, String url, int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    try {
                        JSONArray jsonArray = null;
                        if (nodeValue != null) {
                            jsonArray = new JSONObject(body).getJSONArray(nodeValue);
                        } else {
                            jsonArray = new JSONArray(body);
                        }
                        List<T> items = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            T item = getGson().fromJson(jsonArray.getJSONObject(i).toString(), modelClass);
                            items.add(item);
                        }
                        networkResponse.setResponse(items);
                    } catch (Exception e) {
                        networkResponse.setErrorMsg(e.toString());
                    }
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public <T> void getAll(final String url, final IConnectionListenerEx listener) {
        getAll(url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void getAll(String url, int requestID, final IConnectionListenerEx listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    try {
                        JSONArray jsonArray = null;
                        if (nodeValue != null) {
                            jsonArray = new JSONObject(body).getJSONArray(nodeValue);
                        } else {
                            jsonArray = new JSONArray(body);
                        }
                        List<T> items = getGson().fromJson(jsonArray.toString(), listener.withType());
//                        for (int i = 0; i < jsonArray.length(); i++) {
//                            T item = getGson().fromJson(jsonArray.getJSONObject(i).toString(), modelClass);
//                            items.add(item);
//                        }
                        networkResponse.setResponse(items);
                    } catch (Exception e) {
                        networkResponse.setErrorMsg(e.toString());
                    }
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public <T> void getRaw(String url, final int requestID, final IConnectionListenerEx listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    try {
                        T item = getGson().fromJson(body, listener.withType());
                        networkResponse.setResponse(item);
                    } catch (Exception e) {
                        networkResponse.setErrorMsg(e.toString());
                    }
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public NetworkResponse count(String url) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            Request request = genRequest(url, NetworkHttpMethod.GET, null);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            boolean valid = validate(NetworkHttpMethod.GET, code);
            networkResponse.setSuccess(valid);
            if (valid) {
                networkResponse.setResponse(Integer.parseInt(body));
            }
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public void count(String url, final IConnectionListener listener) {
        count(url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public void count(String url, int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.GET;
        Request request = genRequest(url, method, null);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                if (valid) {
                    networkResponse.setResponse(Integer.parseInt(body));
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public <T> NetworkResponse post(Class<T> modelClass, String url, T item) {
        NetworkResponse networkResponse = new NetworkResponse();
        final NetworkHttpMethod method = NetworkHttpMethod.POST;
        try {
            Request request = genRequest(url, method, getGson().toJson(item, modelClass));
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            boolean validate = validate(method, code);
            networkResponse.setSuccess(validate);
            if (validate) {
                networkResponse.setResponse(getGson().fromJson(body, modelClass));
            } else {
                networkResponse.setErrorMsg("" + body);
            }
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public NetworkResponse post(String url, String jsonBody) {
        NetworkResponse networkResponse = new NetworkResponse();
        final NetworkHttpMethod method = NetworkHttpMethod.POST;
        try {
            Request request = genRequest(url, method, jsonBody);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            boolean validate = validate(method, code);
            networkResponse.setSuccess(validate);
            networkResponse.setResponse("" + body);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public NetworkResponse post(String url, RequestBody requestBody) {
        NetworkResponse networkResponse = new NetworkResponse();
        final NetworkHttpMethod method = NetworkHttpMethod.POST;
        try {
            Request request = genRequestWithBody(url, method, requestBody);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(method, code));
            networkResponse.setResponse("" + body);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public NetworkResponse post(String url, ODataBatchRequest batchRequest) {

        NetworkResponse networkResponse = new NetworkResponse();
        final NetworkHttpMethod method = NetworkHttpMethod.POST;

        try {

            final String contentType = "multipart/mixed; boundary=" + batchRequest.getBoundaryName();

            RequestBody requestBody = new MultipartBody.Builder("")
                    .addPart(RequestBody.create(null, batchRequest.rawBody()))
                    .build();

            Request.Builder builder = new Request.Builder().url(url).headers(getDefaultHeaders(contentType));
            Request request = builder.post(requestBody).build();

            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(method, code));
            networkResponse.setResponse("" + body);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public <T> void post(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener) {
        post(modelClass, url, item, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void post(final Class<T> modelClass, String url, T item, int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.POST;
        Request request = genRequest(url, method, getGson().toJson(item, modelClass));
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean validate = validate(method, code);
                networkResponse.setSuccess(validate);
                if (validate) {
                    networkResponse.setResponse(getGson().fromJson(body, modelClass));
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public void post(String url, String jsonBody, IConnectionListener listener) {
        post(url, jsonBody, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public void post(String url, String jsonBody, int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.POST;
        Request request = genRequest(url, method, jsonBody);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean validate = validate(method, code);
                networkResponse.setSuccess(validate);
                if (validate) {
                    networkResponse.setResponse("" + body);
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    networkResponse.setErrorMsg("" + body);
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    @Override
    public void post(String url, RequestBody requestBody, IConnectionListener listener) {
        post(url, requestBody, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public void post(String url, RequestBody requestBody, int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.POST;
        Request request = genRequestWithBody(url, method, requestBody);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                networkResponse.setSuccess(validate(method, code));
                networkResponse.setResponse("" + body);
                publishSuccess(listener, call, networkResponse);
            }
        });
    }

    @Override
    public void post(String url, ODataBatchRequest batchRequest, IConnectionListener listener) {
        post(url, batchRequest, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public void post(String url, ODataBatchRequest batchRequest, int requestID, final IConnectionListener listener) {

        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        final NetworkHttpMethod method = NetworkHttpMethod.POST;

        final String contentType = "multipart/mixed; boundary=" + batchRequest.getBoundaryName();

        RequestBody requestBody = new MultipartBody.Builder("")
                .addPart(RequestBody.create(null, batchRequest.rawBody()))
                .build();

        Request.Builder builder = new Request.Builder().url(url).headers(getDefaultHeaders(contentType));
        Request request = builder.post(requestBody).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                networkResponse.setSuccess(validate(method, code));
                networkResponse.setResponse("" + body);
                publishSuccess(listener, call, networkResponse);
            }
        });

    }

    @Override
    public <T> NetworkResponse put(Class<T> modelClass, String url, T item) {
        return update(NetworkHttpMethod.PUT, url, getGson().toJson(item, modelClass));
    }

    @Override
    public <T> void put(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener) {
        put(modelClass, url, item, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void put(Class<T> modelClass, String url, T item, int requestID, IConnectionListener listener) {
        update(NetworkHttpMethod.PUT, url, getGson().toJson(item, modelClass), requestID, listener);
    }

    @Override
    public <T> NetworkResponse patch(Class<T> modelClass, String url, T item) {
        return update(NetworkHttpMethod.PATCH, url, getGson().toJson(item, modelClass));
    }

    @Override
    public <T> void patch(final Class<T> modelClass, final String url, final T item, final IConnectionListener listener) {
        patch(modelClass, url, item, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void patch(Class<T> modelClass, String url, T item, int requestID, IConnectionListener listener) {
        update(NetworkHttpMethod.PATCH, url, getGson().toJson(item, modelClass), requestID, listener);
    }

    @Override
    public <T> NetworkResponse delete(Class<T> modelClass, String url, T item) {
        return update(NetworkHttpMethod.DELETE, url, getGson().toJson(item, modelClass));
    }

    @Override
    public <T> void delete(Class<T> modelClass, String url, T item, IConnectionListener listener) {
        delete(modelClass, url, item, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public <T> void delete(Class<T> modelClass, String url, T item, int requestID, IConnectionListener listener) {
        update(NetworkHttpMethod.DELETE, url, getGson().toJson(item, modelClass), requestID, listener);
    }

    @Override
    public NetworkResponse delete(String url) {
        return update(NetworkHttpMethod.DELETE, url, null);
    }

    @Override
    public void delete(String url, IConnectionListener listener) {
        delete(url, NetworkResponse.DEFAULT_REQUEST_ID, listener);
    }

    @Override
    public void delete(String url, int requestID, IConnectionListener listener) {
        update(NetworkHttpMethod.DELETE, url, null, requestID, listener);
    }

    @Override
    public <T> NetworkResponse update(final NetworkHttpMethod method, String url, String content) {
        NetworkResponse networkResponse = new NetworkResponse();
        try {
            Request request = genRequest(url, method, content);
            Response response = okHttpClient.newCall(request).execute();
            final int code = response.code();
            final String body = response.body().string();
            networkResponse.setStatusCode(code);
            networkResponse.setSuccess(validate(method, code));
            networkResponse.setResponse("" + body);
        } catch (Exception e) {
            networkResponse.setSuccess(false);
            networkResponse.setErrorMsg(e.toString());
        }
        return networkResponse;
    }

    @Override
    public <T> void update(final NetworkHttpMethod method, final String url, final String content, final int requestID, final IConnectionListener listener) {
        final NetworkResponse networkResponse = new NetworkResponse(requestID);
        Request request = genRequest(url, method, content);
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                networkResponse.setSuccess(false);
                networkResponse.setErrorMsg(e.toString());
                //listener.onFailure(call, networkResponse);
                publishFailure(listener, call, networkResponse);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();
                final String body = response.body().string();
                networkResponse.setStatusCode(code);
                boolean valid = validate(method, code);
                networkResponse.setSuccess(valid);
                networkResponse.setResponse("" + body);
                if (valid) {
                    //listener.onResponse(call, networkResponse);
                    publishSuccess(listener, call, networkResponse);
                } else {
                    //listener.onFailure(call, networkResponse);
                    publishFailure(listener, call, networkResponse);
                }
            }
        });
    }

    private Request genRequest(final String url, NetworkHttpMethod method, String jsonBody) {
        Request.Builder builder = new Request.Builder().url(url).headers(getDefaultHeaders());
        RequestBody requestBody = null;
        if (jsonBody != null) {
            requestBody = RequestBody.create(getJsonMediaType(), jsonBody);
        }
        switch (method) {
            case GET:
                builder.get();
                break;
            case PUT:
                builder.put(requestBody);
                break;
            case POST:
                builder.post(requestBody);
                break;
            case PATCH:
                builder.patch(requestBody);
                break;
            case DELETE:
                if (requestBody != null) {
                    builder.delete(requestBody);
                } else {
                    builder.delete();
                }
                break;
        }
        return builder.build();
    }

    private Request genRequestWithBody(final String url, NetworkHttpMethod method, RequestBody requestBody) {
        Request.Builder builder = new Request.Builder().url(url).headers(getDefaultHeaders());
        switch (method) {
            case GET:
                builder.get();
                break;
            case PUT:
                builder.put(requestBody);
                break;
            case POST:
                builder.post(requestBody);
                break;
            case PATCH:
                builder.patch(requestBody);
                break;
            case DELETE:
                if (requestBody != null) {
                    builder.delete(requestBody);
                } else {
                    builder.delete();
                }
                break;
        }
        return builder.build();
    }

    public static boolean validate(final NetworkHttpMethod method, final int responseCode) {
        boolean success = false;
        switch (method) {
            case DELETE:
            case PATCH:
            case PUT:
                success = (responseCode == ODATAHttpCodes.UPDATE.code() || responseCode == ODATAHttpCodes.REQUEST.code());
                break;
            case POST:
                success = (responseCode == ODATAHttpCodes.CREATE.code() || responseCode == ODATAHttpCodes.REQUEST.code());
                break;
            case GET:
                success = (responseCode == ODATAHttpCodes.REQUEST.code());
                break;
        }
        return success;
    }

    public enum NetworkHttpMethod {
        GET, POST, PUT, PATCH, DELETE
    }

    enum ODATAHttpCodes {
        UPDATE(204),
        CREATE(201),
        REQUEST(200),
        UNAUTHORISED(401);

        private int code;

        ODATAHttpCodes(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

    }

    class OAuth2TokenInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request); //perform request, here original request will be executed
            final int code = response.code();
            if (code == ODATAHttpCodes.UNAUTHORISED.code()) {
                synchronized (okHttpClient) {
                    NetworkResponse networkResponse = refreshToken();
                    if (networkResponse.isSuccess()) {
                        if (refreshTokenListener != null) {
                            refreshTokenListener.refreshed(networkResponse);
                        }
                        return chain.proceed(getNewRequest(request));
                    } else {
                        // App Logout
                        if (refreshTokenListener != null) {
                            refreshTokenListener.needAppLogout(networkResponse);
                        }
                        cancelAllRunningCall();
                        return response;
                    }
                }
            }
            return response;
        }
    }

    private Request getNewRequest(Request request) {
        Request newRequest = request.newBuilder()
                .headers(getDefaultHeaders())
                .build();
        return newRequest;
    }

    private void cancelAllRunningCall() {
        try {
            for (Call call : okHttpClient.dispatcher().runningCalls()) {
                call.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
