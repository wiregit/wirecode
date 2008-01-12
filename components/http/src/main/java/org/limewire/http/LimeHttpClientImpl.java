package org.limewire.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.conn.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * 
 */
public class LimeHttpClientImpl extends DefaultHttpClient implements SocketWrappingClient {
    
    private static final Log LOG = LogFactory.getLog(LimeHttpClientImpl.class);
    
    private static HttpParams defaultParams = new DefaultHttpParams();
    private Socket socket;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void releaseConnection(HttpRequest request, HttpResponse response) {
        close(response);
    }
    
    public LimeHttpClientImpl() {
        super(null, new BasicHttpParams(defaultParams));
    }

    public LimeHttpClientImpl(ClientConnectionManager manager) {
        super(manager, new BasicHttpParams(defaultParams));
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
        // TODO idle connection time
        //((MultiThreadedHttpConnectionManager)MANAGER).setIdleConnectionTime(IDLE_TIME);
        
        // TODO does this need to be ThreadSafe?
        // TODO close idle connections
        return new ThreadSafeClientConnManager(defaultParams, registry);               
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
