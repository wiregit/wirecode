package org.limewire.http;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpParams;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;


/**
 * A factory for retrieving HttpClient instances.
 */
// TODO: move to interface/impl, not static.
public class HttpClientManager {
    
    private static Provider<LimeHttpClient> blockingClient;
    private static Provider<LimeHttpClient> nonBlockingClient;
    private static Provider<SocketWrappingClient> socketWrappingClient;

    /** Ensures this is initialized. */
    @Inject
    public synchronized static void initialize(@Named("blockingClient") Provider<LimeHttpClient> blockingClient,
                                               Provider<LimeHttpClient> nonBlockingClient,
                                               Provider<SocketWrappingClient> socketWrappingClient) {
        HttpClientManager.blockingClient = blockingClient;
        HttpClientManager.nonBlockingClient = nonBlockingClient;
        HttpClientManager.socketWrappingClient = socketWrappingClient;
    }
    
    public static HttpClient getNewClient() {
        return getNewClient(true);
    }
    
    public static HttpClient getNewClient(boolean nio) {
        return getNewClient(null, nio);
    }
    
    public static HttpClient getNewClient(HttpParams params) {
        return getNewClient(params, true);
    }
    
    public static HttpClient getNewClient(Socket socket) {
       return getNewClient(null, socket);
    }
    
    public static HttpClient getNewClient(HttpParams params, Socket socket){
        SocketWrappingClient client = socketWrappingClient.get();
        client.setSocket(socket);
        if(params != null) {
            client.setParams(params);
        }
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
        LimeHttpClient client;
        if(nio) {
            client = nonBlockingClient.get();
        } else {
            client = blockingClient.get();
        }
        if(params != null) {
            client.setParams(params);
        }
        return client;
    }
    
    public static void releaseConnection(HttpResponse response) {
        if(response != null && response.getEntity() != null) {
            try {
                response.getEntity().consumeContent();
                //IOUtils.close(response.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();  // TODO log
            }            
        }
    }        
}
