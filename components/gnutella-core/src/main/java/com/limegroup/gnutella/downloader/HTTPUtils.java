package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.limewire.net.HttpClientManager;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 *  Provides common utility methods that interact with the HTTPClient
 */
public class HTTPUtils {
    
    
    /** Returns the length of the content at the given URL. 
     *  NOTE: This method is blocking. It contacts the url and performs
     *  a head request to return the content length of the file. 
     *  
     *  @exception IOException couldn't find the length for some reason */
    public static long contentLength(URL url) throws IOException {
        try {
            // Verify that the URL is valid.
            new URI(url.toExternalForm().toCharArray());
        } catch(URIException e) {
            //invalid URI, don't allow this URL.
            throw new IOException("invalid url: " + url);
        }

        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod head = new HeadMethod(url.toExternalForm());
        head.addRequestHeader("User-Agent",
                              LimeWireUtils.getHttpServer());
        try {
            client.executeMethod(head);
            //Extract Content-length, but only if the response was 200 OK.
            //Generally speaking any 2xx response is ok, but in this situation
            //we expect only 200.
            if (head.getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Got " + head.getStatusCode() +
                                      " instead of 200");
            
            long length = head.getResponseContentLength();
            if (length<0)
                throw new IOException("No content length");
            return length;
        } finally {
            head.releaseConnection();
        }
    }
}
