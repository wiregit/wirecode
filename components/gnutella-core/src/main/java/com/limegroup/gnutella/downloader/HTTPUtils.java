package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.limewire.http.HttpClientManager;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 *  Provides common utility methods that interact with the HTTPClient
 */
public class HTTPUtils {
    
    
    /** Returns the length of the content at the given URL. 
     *  NOTE: This method is blocking. It contacts the uri and performs
     *  a head request to return the content length of the file. 
     *  
     *  @exception IOException couldn't find the length for some reason */
    public static long contentLength(URI uri) throws IOException, HttpException, InterruptedException {
        HttpClient client = HttpClientManager.getNewClient();
        HttpHead head = new HttpHead(uri);
        head.addHeader("User-Agent",
                LimeWireUtils.getHttpServer());
        HttpResponse response = null;
        try {
            response = client.execute(head);
            //Extract Content-length, but only if the response was 200 OK.
            //Generally speaking any 2xx response is ok, but in this situation
            //we expect only 200.
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Got " + response.getStatusLine().getStatusCode() +
                        " instead of 200 for URL: " + uri);

            long length = -1;
            if (response.getEntity() != null) {
                length = response.getEntity().getContentLength();
            }
            if (length < 0)
                throw new IOException("No content length");
            return length;
        } finally {
            HttpClientManager.releaseConnection(response);
        }
    }
}
