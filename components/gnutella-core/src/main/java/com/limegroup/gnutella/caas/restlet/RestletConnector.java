package com.limegroup.gnutella.caas.restlet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.PlainSocketFactory;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.conn.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class RestletConnector {

    private static HttpParams _httpParams;
    
    private static SchemeRegistry _schemes;
    
    private static ClientConnectionManager _connManager;
    
    private static HttpHost _defaultHost;
    
    static {
        setup();
    }
    
    /**
     * 
     */
    private final static void setup() {
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        
        _schemes = new SchemeRegistry();
        _schemes.register(new Scheme("http", sf, 80));
        _httpParams = new BasicHttpParams();
        _connManager = new ThreadSafeClientConnManager(_httpParams, _schemes);
        
        HttpProtocolParams.setVersion(_httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(_httpParams, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(_httpParams, true);
    }
    
    /**
     * 
     */
    private final static HttpRequest createRequest() {
        HttpRequest hr = new BasicHttpRequest("GET", "/", HttpVersion.HTTP_1_1);
        
        return hr;
    }
    
    /**
     * 
     */
    private final static HttpClient createHttpClient() {
        return new DefaultHttpClient(_connManager, _httpParams);
    }
    
    /**
     * 
     */
    public void setDefaultHost(String host, int port) {
        _defaultHost = new HttpHost(host, port, "http");
    }
    
    public static String sendRequest(String path) throws Exception {
        return sendRequest(_defaultHost, path);
    }
    
    public static String sendRequest(HttpHost host, String path) throws Exception {
        HttpClient client = createHttpClient();
        HttpRequest request = createRequest();
        HttpEntity entity = null;
        StringBuilder sb = new StringBuilder();
        
        try {
            // connect, send, read, etc.
        }
        finally {
            if (entity != null)
                entity.consumeContent();
        }
        
        return sb.toString();
    }
    
}
