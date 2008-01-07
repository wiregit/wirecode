package org.limewire.http;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.*;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.NBSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * A factory for retrieving HttpClient instances.
 */
// TODO: move to interface/impl, not static.
public class HttpClientManager {
    
    private static final Log LOG = LogFactory.getLog(HttpClientManager.class);
    
    
    
    /**
     * The time to allow a connection to sit idle, waiting for something
     * to reuse it.
     */
    private static final long IDLE_TIME = 30 * 1000; // 30 seconds.
    
    /**
     * The manager which all client connections use if not Java 1.1.8;
     */
    private static ClientConnectionManager MANAGER;
    
    private static ClientConnectionManager BLOCKING_MANAGER;
    
    private static Provider<SocketsManager> socketsManager;
    
    private static HttpParams defaultParams;
    
    static {
        initialize();        
    }
    
    /** Ensures this is initialized. */
    @Inject
    public static void initialize() {
        shutdown();
        defaultParams = new DefaultHttpParams();
        
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new LimeSocketFactory(ConnectType.PLAIN), 80));
        registry.register(new Scheme("tls", new LimeSocketFactory(ConnectType.TLS),80));
        registry.register(new Scheme("https", new LimeSocketFactory(ConnectType.TLS),80));
        
        // TODO idle connection time
        //((MultiThreadedHttpConnectionManager)MANAGER).setIdleConnectionTime(IDLE_TIME);
        
        MANAGER = new ThreadSafeClientConnManager(defaultParams, registry);     
        
        registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("tls", SSLSocketFactory.getSocketFactory(),80)); // TODO no ssl?
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(),80)); // TODO no ssl?
        
        // TODO idle connection time
        //((MultiThreadedHttpConnectionManager)MANAGER).setIdleConnectionTime(IDLE_TIME);
        
        BLOCKING_MANAGER = new ThreadSafeClientConnManager(defaultParams, registry);
        //MANAGER = BLOCKING_MANAGER;    
    }
    
    public static void shutdown() {
        if(MANAGER != null) {
            MANAGER.shutdown();
        }
        if(BLOCKING_MANAGER != null) {
            BLOCKING_MANAGER.shutdown();
        }
    }
    
    @Inject public static void setSocketsManager(Provider<SocketsManager> socketsManager) {
        HttpClientManager.socketsManager = socketsManager;
    }
    
    public static HttpClient getNewClient() {
        return getNewClient(true);
    }
    
    public static HttpClient getNewClient(boolean nio) {
        return getNewClient(new BasicHttpParams(defaultParams), nio);
    }
    
    public static HttpClient getNewClient(HttpParams params) {
        return getNewClient(params, true);
    }
    
    public static HttpClient getNewClient(Socket socket) {
       return getNewClient(new BasicHttpParams(defaultParams), socket);
    }
    
    public static HttpClient getNewClient(HttpParams params, Socket socket){
        ClientConnectionManager manager;
        if(socket == null) {
            throw new IllegalArgumentException("null Socket");
        } else {
            manager = createSocketWrappingManager(socket);
        }
        DefaultHttpClient client = new DefaultHttpClient(manager, params);
        client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        return client;
    }
        
    /**
     * Returns a new HttpClient with the appropriate manager and parameters.
     * 
     * @param connectTimeout the number of milliseconds to wait to establish
     *  a TCP connection with the remote host
     * @param soTimeout the socket timeout -- the number of milliseconds to 
     *  wait for data before closing an established socket
     */
    public static HttpClient getNewClient(HttpParams params, boolean nio) {
        ClientConnectionManager manager;
        if(nio) {
            manager = MANAGER;    
        } else {
            manager = BLOCKING_MANAGER;
        }
        DefaultHttpClient client = new DefaultHttpClient(manager, params);
        client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        return client;
        //return new DefaultHttpClient(new ThreadSafeClientConnManager(MANAGER.getParams(), MANAGER.getSchemeRegistry()), params);
    }
    
    public static void close(HttpResponse response) {
        if(response != null && response.getEntity() != null) {
            try {
                response.getEntity().consumeContent();
                //IOUtils.close(response.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();  // TODO log
            }            
        }
    }

    private static ClientConnectionManager createSocketWrappingManager(Socket socket) {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new SocketWrapperProtocolSocketFactory(socket), 80));
        registry.register(new Scheme("tls", new SocketWrapperProtocolSocketFactory(socket),80));
        registry.register(new Scheme("https", new SocketWrapperProtocolSocketFactory(socket),80));
        
        HttpParams defaultParams = new BasicHttpParams();
        // TODO idle connection time
        //((MultiThreadedHttpConnectionManager)MANAGER).setIdleConnectionTime(IDLE_TIME);
        
        // TODO does this need to be ThreadSafe?
        return new ThreadSafeClientConnManager(defaultParams, registry);               
    }
    
    private static class LimeSocketFactory implements SocketFactory {
        private final ConnectType type;
        
        public LimeSocketFactory(ConnectType type) {
            this.type = type;
        }

        public Socket createSocket() throws IOException {
            return socketsManager.get().create(type);
        }

        public Socket connectSocket(Socket socket, String targetHost, int targetPort, InetAddress localAddress, int localPort, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
            if(socket == null) {
                socket = createSocket();
            }            
            return socketsManager.get().connect((NBSocket)socket, localAddress != null ? localAddress.getHostAddress() : null, localPort, new InetSocketAddress(targetHost,targetPort), HttpConnectionParams.getConnectionTimeout(httpParams), type);
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false;  // TODO
        }

        public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) 
          throws IOException, UnknownHostException {
            return socketsManager.get().connect(new InetSocketAddress(host,port),0, type);
        }

        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return socketsManager.get().connect(new InetSocketAddress(host,port),0, type);
        }
        
        public Socket createSocket(String host, int port, int timeout) throws IOException, UnknownHostException {
            return socketsManager.get().connect(new InetSocketAddress(host,port), timeout, type);
        }
        
    }

    private static class SocketWrapperProtocolSocketFactory implements SocketFactory {

        private Socket socket;

        SocketWrapperProtocolSocketFactory(Socket s) {
            socket = s;
        }

        public Socket createSocket() throws IOException {
            return socket; // TODO validate parameters actually match those of the socket
        }

        public Socket connectSocket(Socket socket, String s, int i, InetAddress inetAddress, int i1, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
            return socket; // TODO validate parameters actually match those of the socket
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false; // TODO
        }

        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
            return socket; // TODO validate parameters actually match those of the socket
        }

        public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
            return socket; // TODO validate parameters actually match those of the socket
        }

        public Socket createSocket(String s, int i, int i1) throws IOException, UnknownHostException {
            return socket; // TODO validate parameters actually match those of the socket
        }
    }
    
}
