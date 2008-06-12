package com.limegroup.gnutella.caas.restlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.PlainSocketFactory;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.conn.SocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RestletConnector {

    private static HttpParams _httpParams;
    
    private static SchemeRegistry _schemes;
    
    private static ClientConnectionManager _connManager;
    
    private static HttpHost _defaultHost;
    
    private static DocumentBuilder _builder;
    
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
        
        try {
            _builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException pce) {
            // ...
        }
        
        HttpProtocolParams.setVersion(_httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(_httpParams, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(_httpParams, true);
    }
    
    /**
     * 
     */
    private final static HttpRequest createRequest(String path) {
        HttpRequest hr = new BasicHttpRequest("GET", path, HttpVersion.HTTP_1_1);
        
        return hr;
    }
    
    /**
     * 
     */
    private final static HttpPost createPost(String path) {
        HttpPost post = null;
        
        try {
            post = new HttpPost("http://" + _defaultHost.getHostName() + ":" + _defaultHost.getPort() + path);
        }
        catch (URISyntaxException use) {
            System.err.println("RestletConnector::createPost().. " + use.getMessage());
            use.printStackTrace();
        }
        
        return post;
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
    public final static void setDefaultHost(String host, int port) {
        _defaultHost = new HttpHost(host, port, "http");
    }
    
    /**
     * 
     */
    public final static Document sendRequest(String path) {
        return sendRequest(_defaultHost, path);
    }
    
    /**
     * 
     */
    public final static Document sendRequest(String path, Element data) {
        return sendRequest(_defaultHost, path, data);
    }
    
    /**
     * 
     */
    public final static Document sendRequest(HttpHost host, String path, Element data) {
        HttpClient client = createHttpClient();
        HttpPost post = createPost(path);
        HttpEntity entity = null;
        Document document = null;
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputFormat format = new OutputFormat();
            format.setIndenting(false);
            XMLSerializer serializer = new XMLSerializer(baos, format);
            serializer.serialize(data);
            ByteArrayEntity requestEntity = new ByteArrayEntity(baos.toByteArray());
            requestEntity.setContentType("text/xml");
            requestEntity.setChunked(false);
            post.setEntity(requestEntity);
            
            HttpResponse response = client.execute(host, post, null);
            entity = response.getEntity();
            String strdata = EntityUtils.toString(entity);
            System.out.println("DATA: " + strdata);
//          document = _builder.parse(new ByteArrayInputStream(EntityUtils.toString(entity).getBytes()));
            document = _builder.parse(new ByteArrayInputStream(strdata.getBytes()));
        }
        catch (Exception e) {
            System.out.println("RestletConnector::sendRequest().. [1] " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (entity != null)
                    entity.consumeContent();
            }
            catch (IOException e) {
                System.out.println("RestletConnector::sendRequest().. [2] " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return document;
    }
    
    /**
     * 
     */
    public final static Document sendRequest(HttpHost host, String path) {
        HttpClient client = createHttpClient();
        HttpRequest request = createRequest(path);
        HttpEntity entity = null;
        Document document = null;
        
        try {
            HttpResponse response = client.execute(host, request, null);
            entity = response.getEntity();
            String data = EntityUtils.toString(entity);
            System.out.println("DATA: " + data);
//          document = _builder.parse(new ByteArrayInputStream(EntityUtils.toString(entity).getBytes()));
            document = _builder.parse(new ByteArrayInputStream(data.getBytes()));
        }
        catch (Exception e) {
            System.out.println("RestletConnector::sendRequest().. [1] " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (entity != null)
                    entity.consumeContent();
            }
            catch (IOException e) {
                System.out.println("RestletConnector::sendRequest().. [2] " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return document;
    }
    
}
