package com.limegroup.gnutella.http;

import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.auth.HttpAuthenticator;

import com.limegroup.gnutella.util.CommonUtils;

import java.io.IOException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


/**
 * A simple manager class that maintains a single HttpConnectionManager
 * and doles out either a simple one (for Java 1.1.8) or the MultiThreaded
 * one (for all other versions)
 */
public class HttpClientManager {
    
    private static final Log LOG = LogFactory.getLog(HttpClientManager.class);
    
    /**
     * The amount of time to wait while trying to connect to a specified
     * host.  If we exceed this value, an IOException is thrown
     * while trying to connect.
     */
    private static final int CONNECTION_TIMEOUT = 3000;
    
    /**
     * The amount of time to wait while receiving data from a specified
     * host.  Used as an SO_TIMEOUT.
     */
    private static final int TIMEOUT = 2000;
    
    /**
     * The maximum number of times to allow redirects from hosts.
     */
    private static final int MAXIMUM_REDIRECTS = 10;
    
    /**
     * The manager which all client connections use.
     */
    private static final HttpConnectionManager MANAGER = 
       CommonUtils.isJava118() ?
            (HttpConnectionManager)new SimpleHttpConnectionManager() :
            (HttpConnectionManager)new MultiThreadedHttpConnectionManager();
        // note: the cast is required because of a strange compiler bug that
        // seems to spits out:
        //     "incompatible types for ?: neither is a subtype of the other"
        // even though all logic would dictate that they don't have to
        // to be subtypes of each other
            
    /**
     * Returns a new HttpClient with the appropriate manager.
     */
    public static HttpClient getNewClient() {
        return getNewClient(CONNECTION_TIMEOUT, TIMEOUT);
    }
    
    /**
     * Returns a new HttpClient with the appropriate manager and parameters
     */
    public static HttpClient getNewClient(int connectTimeout, int soTimeout) {
        HttpClient client = new HttpClient(MANAGER);
        client.setConnectionTimeout(connectTimeout);
        client.setTimeout(soTimeout);
        return client;
    }
    
    /**
     * Executes the given HttpMethod in the HttpClient, following redirects.
     * This method is needed because HttpClient does not support redirects
     * across protocols, hosts, and/or ports.
     */
    public static void executeMethodRedirecting(HttpClient client,
                                                HttpMethod methid)
      throws IOException, HttpException {
        executeMethodRedirecting(client, methid, MAXIMUM_REDIRECTS);
    }
    
    /**
     * Executes the given HttpMethod in the HttpClient, following redirecits
     * up to the specific number of times.
     * This method is needed because HttpClient does not support redirects
     * across protocols, hosts, and/or ports.
     */
    public static void executeMethodRedirecting(HttpClient client,
                                                HttpMethod methid,
                                                int redirects)
      throws IOException, HttpException {
        for(int i = 0; i < redirects; i++) {
            if(LOG.isInfoEnabled())
                LOG.info("Attempting connection (" + i + ") to " + 
                         methid.getURI().getEscapedURI());
            client.executeMethod(methid);
            switch(methid.getStatusCode()) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
                if(!methid.getFollowRedirects()) {
                    if(LOG.isInfoEnabled())
                        LOG.warn("Redirect requested but not supported");
                    throw new HttpException("Redirect requested");
                }
                
                Header locationHeader = methid.getResponseHeader("location");
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
                Header[] requestHeaders = methid.getRequestHeaders();
                
                // Recycle this method so we can use it again.
                methid.recycle();
                
                HostConfiguration hc = methid.getHostConfiguration();
                hc.setHost(
                    newLocation.getHost(),
                    newLocation.getPort(),
                    newLocation.getScheme()
                );
                
                methid.setFollowRedirects(true);
                
                for(int j = 0; j < requestHeaders.length; j++) {
                    if(!requestHeaders[j].getName().equals("Host"))
                        methid.addRequestHeader(requestHeaders[j]);
                }
                
                // Set up the new values for the method.
                methid.setPath(newLocation.getEscapedPath());
                methid.setQueryString(newLocation.getEscapedQuery());
                methid.removeRequestHeader(HttpAuthenticator.WWW_AUTH_RESP);
                // Release the existing connection... we know it is not
                // the same host/port/protocol, because if it was,
                // it would be handled internally by HttpClient.
                methid.releaseConnection();
                // Loop around and try the method again.
                break;
            default:
                return;
            }
        }
        throw new HttpException("Maximum redirects encountered, bailing");
    }
    
}
