package com.neoplay.tv.utils;

import android.content.Context;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.TimeUnit;

@UnstableApi
public class NetworkUtils {

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            
            builder.connectTimeout(15, TimeUnit.SECONDS);
            builder.readTimeout(20, TimeUnit.SECONDS);
            builder.followRedirects(true);
            builder.followSslRedirects(true);

            // Dinamik Header Interceptor (Referer və Origin əlavə edir)
            builder.addInterceptor(chain -> {
                Request original = chain.request();
                String host = original.url().host();
                String scheme = original.url().scheme();
                
                Request.Builder requestBuilder = original.newBuilder()
                        .header("User-Agent", "IPTVSmartersPlayer")
                        .header("Accept", "*/*")
                        .header("Referer", scheme + "://" + host + "/")
                        .header("Origin", scheme + "://" + host);
                
                return chain.proceed(requestBuilder.build());
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static OkHttpDataSource.Factory getDataSourceFactory(Context context) {
        return new OkHttpDataSource.Factory(getUnsafeOkHttpClient())
                .setUserAgent("IPTVSmartersPlayer");
    }
}
