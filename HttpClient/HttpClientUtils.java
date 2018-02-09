import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * HttpClient工具
 */
public class HttpClientUtils {
    /**
     * 对https访问信任，不需要证书；
     * 设置http连接池的最大连接数，和同一个主机(IP)的最大并发连接数
     *
     * @param proxyHost 代理ip
     * @param proxyPort      代理端口
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static CloseableHttpClient acceptsUntrustedCertsHttpClient(String proxyHost, int proxyPort)
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        // setup a Trust Strategy that allows all certificates.
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        }).build();

        httpClientBuilder.setSSLContext(sslContext);

        // don't check Hostnames, either.
        // use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

        // here's the special part:
        // -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
        // -- and create a Registry, to register it.
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory).build();
        // now, we create connection-manager using our Registry. allows multi-threaded use
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connMgr.setMaxTotal(200);
        connMgr.setDefaultMaxPerRoute(100);

        httpClientBuilder.setConnectionManager(connMgr);
        if (StringUtils.isNotBlank(proxyHost)) {
            HttpHost httpHost = new HttpHost(proxyHost, proxyPort);
            httpClientBuilder.setProxy(httpHost);
        }

        // finally, build the HttpClient;
        CloseableHttpClient client = httpClientBuilder.build();
        return client;
    }
    
    public static CloseableHttpClient createHttpClientWithCert(KeyStore keyStore, String keyStorePassword,
			KeyStore trustStoreFile, int connMaxTotal, int connDefaultMaxPerRoute, int validateInactivityMillSeconds,
			int connEvictIdleConnectionsTimeoutMillSeconds, String proxyHost, int proxyPort, String proxyUsername,
			String proxyPassword) {
		SSLContext sslcontext = null;
		try {
			if(trustStoreFile==null){
				sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, keyStorePassword.toCharArray()).build();
			}else{
				sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
						.loadTrustMaterial(trustStoreFile, new TrustSelfSignedStrategy()).build();
			}
		} catch (Exception e) {
			throw new RuntimeException("key store fail", e);
		}

		// Create all-trusting host name verifier
		/*HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};*/

		// Allow TLSv1 protocol only
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.getDefaultHostnameVerifier());
		// allHostsValid);

		// Create a registry of custom connection socket factories for supported
		// protocol schemes.
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("https", sslsf).register("http", PlainConnectionSocketFactory.INSTANCE).build();

		// Create a connection manager with custom configuration.
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		// Create socket configuration
		SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();// 小数据网络包
		// Configure the connection manager to use socket configuration either
		// by default or for a specific host.
		connManager.setDefaultSocketConfig(socketConfig);
		// Validate connections after 1 sec of inactivity
		connManager.setValidateAfterInactivity(validateInactivityMillSeconds);

		// Configure total max or per route limits for persistent connections
		// that can be kept in the pool or leased by the connection manager.
		connManager.setMaxTotal(connMaxTotal);
		connManager.setDefaultMaxPerRoute(connDefaultMaxPerRoute);

		// Use custom cookie store if necessary.
		CookieStore cookieStore = new BasicCookieStore();
		// Use custom credentials provider if necessary.
		// CredentialsProvider credentialsProvider = new
		// BasicCredentialsProvider();
		// Create global request configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
				.setExpectContinueEnabled(true).build();

		HttpHost proxy = null;
		if (StringUtils.isNotEmpty(proxyHost)) {
			proxy = new HttpHost(proxyHost, proxyPort, "http");
		}
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		if (StringUtils.isNotEmpty(proxyUsername) && StringUtils.isNotEmpty(proxyPassword)) {
			credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUsername,
					proxyPassword));
		}

		// Create an HttpClient with the given custom dependencies and
		// configuration.
		CloseableHttpClient httpclient;
		if (proxy == null) {
			httpclient = HttpClients.custom().setConnectionManager(connManager).setDefaultCookieStore(cookieStore)
					.setDefaultRequestConfig(defaultRequestConfig).evictExpiredConnections()
					.evictIdleConnections(connEvictIdleConnectionsTimeoutMillSeconds, TimeUnit.MILLISECONDS)
					.setSSLSocketFactory(sslsf).build();
		} else {
			httpclient = HttpClients.custom().setConnectionManager(connManager).setProxy(proxy)
					.setDefaultCredentialsProvider(credsProvider).setDefaultCookieStore(cookieStore)
					.setDefaultRequestConfig(defaultRequestConfig).evictExpiredConnections()
					.evictIdleConnections(connEvictIdleConnectionsTimeoutMillSeconds, TimeUnit.MILLISECONDS)
					.setSSLSocketFactory(sslsf).build();
		}
		return httpclient;
	}
}
