package com.neoplay.tv.utils

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@UnstableApi
object NetworkUtils {

    @JvmStatic
    fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }

            builder.connectTimeout(15, TimeUnit.SECONDS)
            builder.readTimeout(20, TimeUnit.SECONDS)
            builder.followRedirects(true)
            builder.followSslRedirects(true)

            // Dinamik Header Interceptor (Referer və Origin əlavə edir)
            builder.addInterceptor { chain ->
                val original = chain.request()
                val host = original.url.host
                val scheme = original.url.scheme

                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "IPTVSmartersPlayer")
                    .header("Accept", "*/*")
                    .header("Referer", "$scheme://$host/")
                    .header("Origin", "$scheme://$host")

                chain.proceed(requestBuilder.build())
            }

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun getDataSourceFactory(context: Context): OkHttpDataSource.Factory {
        return OkHttpDataSource.Factory(getUnsafeOkHttpClient())
            .setUserAgent("IPTVSmartersPlayer")
    }
}
