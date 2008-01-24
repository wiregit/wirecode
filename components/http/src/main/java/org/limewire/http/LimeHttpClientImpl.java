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
 * <code>SocketWrappingHttpClient</code> 
 */
class LimeHttpClientImpl extends DefaultHttpClient implements SocketWrappingHttpClient {
    
    private static final Log LOG = LogFactory.getLog(LimeHttpClientImpl.class);

    public void setSocket(Socket socket) {
        ((ReapingClientConnectionManager)getConnectionManager()).setSocket(socket);
    }

    public void releaseConnection(HttpRequest request, HttpResponse response) {
        close(response);
    }
    
    public LimeHttpClientImpl(ReapingClientConnectionManager manager) {
        super(manager, new BasicHttpParams(new DefaultHttpParams()));
    }

    /**
     * @return an <code>HttpRequestRetryHandler</code> that always returns
     * <code>false</code>
     */
    protected HttpRequestRetryHandler createHttpRequestRetryHandler() {
        return new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                // when requests fail for unexpected reasons (eg., IOException), we don't 
                // want to blindly re-attempt 
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

        
}
