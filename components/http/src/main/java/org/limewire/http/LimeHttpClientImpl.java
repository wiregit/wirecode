package org.limewire.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.Periodic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An <code>HttpClient</code> extension that supports utility methods defined
 * in <code>LimeHttpClient</code> and Socket "injection" as defined in
 * <code>SocketWrappingClient</code> 
 */
class LimeHttpClientImpl extends DefaultHttpClient implements SocketWrappingClient {
    
    private static final Log LOG = LogFactory.getLog(LimeHttpClientImpl.class);
    
    private static HttpParams defaultParams = new DefaultHttpParams();
    private Socket socket;
    private final ScheduledExecutorService scheduler;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void releaseConnection(HttpRequest request, HttpResponse response) {
        close(response);
    }
    
    public LimeHttpClientImpl(ScheduledExecutorService scheduler) {
        this(null, scheduler);
    }

    public LimeHttpClientImpl(ClientConnectionManager manager, ScheduledExecutorService scheduler) {
        super(manager, new BasicHttpParams(defaultParams));
        this.scheduler = scheduler;
    }

    protected ClientConnectionManager createClientConnectionManager() {
        if(socket == null) {
            throw new IllegalStateException("attempt to create ClientConnectionManager with null socket");
        } else {
            return createSocketWrappingManager(socket);
        }
    }

    protected HttpRequestRetryHandler createHttpRequestRetryHandler() {
        return new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                return false;
            }
        };
    }

    public static void close(HttpResponse response) {
        if(response != null && response.getEntity() != null) {
            try {
                response.getEntity().consumeContent();
                //IOUtils.close(response.getEntity().getContent());
            } catch (IOException e) {
                LOG.debug(e.toString(), e);
            }            
        }
    }

    private ClientConnectionManager createSocketWrappingManager(Socket socket) {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new SocketWrapperProtocolSocketFactory(socket), 80));
        registry.register(new Scheme("tls", new SocketWrapperProtocolSocketFactory(socket),80));
        registry.register(new Scheme("https", new SocketWrapperProtocolSocketFactory(socket),80));
        
        HttpParams defaultParams = new BasicHttpParams();
        
        // TODO does this need to be ThreadSafe?
        // TODO does this need to be shutdown ever?
        ClientConnectionManager manager =  new ThreadSafeClientConnManager(defaultParams, registry);
        // TODO does this need to be canceled ever?
        Periodic periodic = new Periodic(new IdleConnectionCloser(manager), scheduler);
        periodic.scheduleAtFixedRate(0, 10, TimeUnit.SECONDS);
        return manager;
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
    }    
}
