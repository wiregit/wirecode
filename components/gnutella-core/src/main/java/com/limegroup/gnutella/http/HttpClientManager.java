package com.limegroup.gnutella.http;

import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpClient;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * A simple manager class that maintains a single HttpConnectionManager
 * and doles out either a simple one (for Java 1.1.8) or the MultiThreaded
 * one (for all other versions)
 */
public class HttpClientManager {
    
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
        HttpClient client = new HttpClient(MANAGER);
        client.setConnectionTimeout(CONNECTION_TIMEOUT);
        client.setTimeout(TIMEOUT);
        return client;
    }
}