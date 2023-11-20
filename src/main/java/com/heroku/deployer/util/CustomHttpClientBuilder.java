package com.heroku.deployer.util;

import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.TextUtils;

import javax.net.ssl.SSLSocketFactory;

public class CustomHttpClientBuilder {

    public static CloseableHttpClient build() {
        /*
        Workaround for JDK-8220723 (https://bugs.openjdk.java.net/browse/JDK-8220723)
        We limit the available protocols to TLS 1.2 to avoid triggering the bug with TLS 1.3.

        Version 11.0.2 is significant to us as it is the default OpenJDK version on Travis CI for Java 11. Since running
        on CI/CD is one of the main use-cases for this library, we can justify this workaround for a bug in an older
        version of the JDK.

        As soon as 11.0.2 is no longer the default on Travis please consider removing this workaround.

        Issue: https://github.com/heroku/heroku-maven-plugin/issues/71
        */
        if (System.getProperty("java.version").equals("11.0.2")) {
            final String[] supportedProtocols = new String[] { "TLSv1.2" };
            final String[] supportedCipherSuites = split(System.getProperty("https.cipherSuites"));

            LayeredConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                    (SSLSocketFactory) SSLSocketFactory.getDefault(),
                    supportedProtocols, supportedCipherSuites, new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault()));

            return HttpClientBuilder
                    .create()
                    .useSystemProperties()
                    .setSSLSocketFactory(sslConnectionSocketFactory)
                    .build();
        }

        return HttpClients.createSystem();
    }

    private static String[] split(final String s) {
        if (TextUtils.isBlank(s)) {
            return null;
        }
        return s.split(" *, *");
    }
}
