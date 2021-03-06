package com.couchmate.teamcity.phabricator;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by david on 8/7/16.
 */
public class HttpClient {
    private boolean selfSigned;

    public HttpClient() {
        selfSigned = false;
    }

    public HttpClient(boolean trustSelfSigned) {
        selfSigned = trustSelfSigned;
    }

    public CloseableHttpClient getCloseableHttpClient(Logger log) {
        SSLContext sslContext;
        try {
            if(selfSigned) {
                sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        return true;
                    }
                }).build();
            } else {
                sslContext = SSLContext.getInstance("SSL");
            }
            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            final Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
            return httpClient;
        } catch (Exception e) {
            log.warn("http client error", e);
            return null;
        }
    }
}
