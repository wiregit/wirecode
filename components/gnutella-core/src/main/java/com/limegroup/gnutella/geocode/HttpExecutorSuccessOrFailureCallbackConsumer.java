/**
 * 
 */
package com.limegroup.gnutella.geocode;

import java.io.IOException;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Provider;
import com.limegroup.gnutella.SuccessOrFailureCallback;
import com.limegroup.gnutella.SuccessOrFailureCallbackConsumer;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * An instance of {@link SuccessOrFailureCallbackConsumer<String>} that will
 * make an HTTP request to a given URL using the injected {@link HttpExecutor}
 * and call {@link SuccessOrFailureCallback#setInvalid(Throwable)} on failure
 * and {@link SuccessOrFailureCallback#process(Object)} on success.
 */
final class HttpExecutorSuccessOrFailureCallbackConsumer implements SuccessOrFailureCallbackConsumer<String> {
    
    private final static Log LOG = LogFactory.getLog(HttpExecutorSuccessOrFailureCallbackConsumer.class);    

    private final Provider<HttpExecutor> exe;
    private final String url;
    private final int timeout;
    
    HttpExecutorSuccessOrFailureCallbackConsumer(Provider<HttpExecutor> exe, String url, int timeout) {
        this.exe = exe;
        this.url = url;
        this.timeout = timeout;        
    }
    
    public void consume(final SuccessOrFailureCallback<String> callback) {
        GetMethod get = new GetMethod(url);
        try {
            if (get.getURI().getHost() == null) {
                throw new IOException("null host!");
            }
        } catch (URIException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        }
        get.addRequestHeader("User-Agent", LimeWireUtils.getHttpServer());
        exe.get().execute(get, new HttpClientListener() {

            public boolean requestComplete(HttpMethod method) {
                callback.process(method.getResponseBodyAsString());
                exe.get().releaseResources(method);
                return false;
            }

            public boolean requestFailed(HttpMethod method, IOException exc) {
                exe.get().releaseResources(method);
                callback.setInvalid(exc);
                return false;
            }

        }, timeout);
    }
}