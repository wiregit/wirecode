package com.limegroup.gnutella.geocode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.limewire.geocode.SuccessOrFailureCallback;
import org.limewire.geocode.SuccessOrFailureCallbackConsumer;

import com.google.inject.Provider;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * An instance of {@link SuccessOrFailureCallbackConsumer<String>} that will
 * make an HTTP request to a given URL using the injected {@link HttpExecutor}
 * and call {@link SuccessOrFailureCallback#setInvalid(java.lang.Throwable)} on failure
 * and {@link SuccessOrFailureCallback#process(Object)} on success.
 */
final class HttpExecutorSuccessOrFailureCallbackConsumer implements SuccessOrFailureCallbackConsumer<InputStream> {
    
    private final static Log LOG = LogFactory.getLog(HttpExecutorSuccessOrFailureCallbackConsumer.class);    

    private final Provider<HttpExecutor> exe;
    private final String url;
    
    HttpExecutorSuccessOrFailureCallbackConsumer(Provider<HttpExecutor> exe, String url) {
        this.exe = exe;
        this.url = url;   
    }
    
    public void consume(final SuccessOrFailureCallback<InputStream> callback) {
        HttpGet get = null;
        try {
            get = new HttpGet(url);
        } catch (URISyntaxException e) {
            LOG.error(e);
            return;
        }
        try {
            if (get.getURI().getHost() == null) {
                throw new IOException("null host!");
            }
        } catch (IOException e) {
            LOG.error(e);
        }
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        exe.get().execute(get, new BasicHttpParams(), new HttpClientListener() {

            public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
                try {
                    callback.process(response.getEntity().getContent());
                } catch (IllegalStateException e) {
                    LOG.error(e);
                } catch (IOException e) {
                    LOG.error(e);
                }
                exe.get().releaseResources(response);
                return false;
            }

            public boolean requestFailed(HttpUriRequest request, HttpResponse response,
                    IOException exc) {
                exe.get().releaseResources(response);
                callback.setInvalid(exc);
                return false;
            }

        });
    }
}