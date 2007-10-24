package com.limegroup.gnutella.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.auth.HttpAuthenticator;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;

import com.google.inject.Inject;
import com.google.inject.Provider;


/**
 * A simple manager class that maintains a single HttpConnectionManager
 * and doles out either a simple one (for Java 1.1.8) or the MultiThreaded
 * one (for all other versions)
 */
public class HttpClientManager {
    
    private static final Log LOG = LogFactory.getLog(HttpClientManager.class);
    
    /**
     * The amount of time to wait while trying to connect to a specified
     * host via TCP.  If we exceed this value, an IOException is thrown
     * while trying to connect.
     */
    private static final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * The amount of time to wait while receiving data from a specified
     * host.  Used as an SO_TIMEOUT.
     */
    private static final int TIMEOUT = 8000;
    
    /**
     * The maximum number of times to allow redirects from hosts.
     */
    private static final int MAXIMUM_REDIRECTS = 10;
    
    /**
     * The time to allow a connection to sit idle, waiting for something
     * to reuse it.
     */
    private static final long IDLE_TIME = 30 * 1000; // 30 seconds.
    
    /**
     * The manager which all client connections use if not Java 1.1.8;
     */
    private static final HttpConnectionManager MANAGER;
    
    @Inject
    static Provider<SocketsManager> socketsManager; 
    
    static {
        MANAGER = new MultiThreadedHttpConnectionManager();
        ((MultiThreadedHttpConnectionManager)MANAGER).setIdleConnectionTime(IDLE_TIME);
        Protocol p = new Protocol("http", new LimeSocketFactory(ConnectType.PLAIN), 80);
        Protocol.registerProtocol("http", p);
        p = new Protocol("tls", new LimeSocketFactory(ConnectType.TLS),80);
        Protocol.registerProtocol("tls", p);
        p = new Protocol("https", new LimeSocketFactory(ConnectType.TLS),80);
        Protocol.registerProtocol("https", p);
    }
    
    /** Ensures this is initialized. */
    public static void initialize() {}
            
    /** Returns a new HttpClient with the appropriate manager. */
    public static HttpClient getNewClient() {
        return getNewClient(CONNECTION_TIMEOUT, TIMEOUT);
    }    
    
    /**
     * Returns a new HttpClient with the appropriate manager and parameters.
     * 
     * @param connectTimeout the number of milliseconds to wait to establish
     *  a TCP connection with the remote host
     * @param soTimeout the socket timeout -- the number of milliseconds to 
     *  wait for data before closing an established socket
     */
    public static HttpClient getNewClient(int connectTimeout, int soTimeout) {
        HttpClient client = new HttpClient(MANAGER);
        client.setConnectionTimeout(connectTimeout);
        client.setTimeout(soTimeout);
        return client;
    }
    
    /**
     * Execute a method without using NIO. 
     */
    public static void executeMethodNoNIO(HttpClient client, HttpMethod method)
      throws IOException, HttpException {
        HostConfiguration hc = method.getHostConfiguration();
        Protocol p = hc == null ? null : hc.getProtocol();
        String scheme = p == null ? "" : p.getScheme();
        if(!"http".equals(scheme.toLowerCase()))
            throw new IOException("only support no NIO with http");
        Protocol http = new Protocol("http", new DefaultProtocolSocketFactory(), 80);
        assert hc != null;
        hc.setHost(hc.getHost(), hc.getPort(), http);
        client.executeMethod(method);
    }
    
    /**
     * Executes the given HttpMethod in the HttpClient, following redirects.
     * This method is needed because HttpClient does not support redirects
     * across protocols, hosts, and/or ports.
     */
    public static void executeMethodRedirecting(HttpClient client,
                                                HttpMethod method)
      throws IOException, HttpException {
        executeMethodRedirecting(client, method, MAXIMUM_REDIRECTS);
    }
    
    /**
     * Executes the given HttpMethod in the HttpClient, following redirecits
     * up to the specific number of times.
     * This method is needed because HttpClient does not support redirects
     * across protocols, hosts, and/or ports.
     */
    public static void executeMethodRedirecting(HttpClient client,
                                                HttpMethod method,
                                                int redirects)
      throws IOException, HttpException {
        executeMethodRedirecting(client, method, redirects, true);
    }
    
    public static void executeMethodRedirectingNoNIO(HttpClient client,
                                                     HttpMethod method,
                                                     int redirects)
      throws IOException, HttpException {
        executeMethodRedirecting(client, method, redirects, false);
    }
    
    private static void executeMethodRedirecting(final HttpClient client,
                                                 final HttpMethod method,
                                                 final int redirects,
                                                 final boolean allowNIO)
      throws IOException, HttpException {
        for(int i = 0; i < redirects; i++) {
            if(LOG.isInfoEnabled())
                LOG.info("Attempting connection (" + i + ") to " + 
                        method.getURI().getEscapedURI());
            try {
                if(allowNIO)
                    client.executeMethod(method);
                else
                    executeMethodNoNIO(client, method);
            } catch(IllegalArgumentException iae) {
                // HttpClient 2.1rc2 has errors w/ parsing cookies.
                // See: https://www.limewire.org/jira/browse/CORE-33
                throw (IOException)new IOException().initCause(iae);
            }
            switch(method.getStatusCode()) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
                if(!method.getFollowRedirects()) {
                    if(LOG.isInfoEnabled())
                        LOG.warn("Redirect requested but not supported");
                    throw new HttpException("Redirect requested");
                }
                
                Header locationHeader = method.getResponseHeader("location");
                if(locationHeader == null) {
                    if(LOG.isInfoEnabled())
                        LOG.warn("Redirect requested, no location header");
                    throw new HttpException("Redirected without a location");
                }
                
                String location = locationHeader.getValue();
                if(LOG.isInfoEnabled())
                    LOG.info("Redirected requested to: " + location);

                URI newLocation = new URI(location.toCharArray());
                
                // Retrieve the RequestHeaders
                Header[] requestHeaders = method.getRequestHeaders();
                
                // Recycle this method so we can use it again.
                method.recycle();
                
                HostConfiguration methodConfig = method.getHostConfiguration();
                    
                methodConfig.setHost(
                    newLocation.getHost(),
                    newLocation.getPort(),
                    newLocation.getScheme()
                );
                
                method.setFollowRedirects(true);
                
                for(int j = 0; j < requestHeaders.length; j++) {
                    if(!requestHeaders[j].getName().equals("Host"))
                        method.addRequestHeader(requestHeaders[j]);
                }
                
                // Set up the new values for the method.
                method.setPath(newLocation.getEscapedPath());
                method.setQueryString(newLocation.getEscapedQuery());
                method.removeRequestHeader(HttpAuthenticator.WWW_AUTH_RESP);

                // Loop around and try the method again.
                break;
            default:
                return;
            }
        }
        throw new HttpException("Maximum redirects encountered, bailing");
    }
    
    private static class LimeSocketFactory implements ProtocolSocketFactory {
        private final ConnectType type;
        
        public LimeSocketFactory(ConnectType type) {
            this.type = type;
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
    
}
