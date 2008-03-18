package com.limegroup.gnutella;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.promotion.AbstractPromotionBinderRequestor;
import org.limewire.promotion.PromotionBinderFactory;
import org.limewire.promotion.impressions.ByteArrayCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class PromotionBinderRequestorImpl extends AbstractPromotionBinderRequestor {
    
    private final static Log LOG = LogFactory.getLog(PromotionBinderRequestorImpl.class);
    
    private final HttpExecutor exe;
    
    @Inject
    public PromotionBinderRequestorImpl(PromotionBinderFactory binderFactory, HttpExecutor exe) {
        super(binderFactory);
        this.exe = exe;
    }

    @Override
    protected void error(Exception e) {
        LOG.error("Error processing promotion binder", e);
    }

    @Override
    protected String getUserAgent() {
        return LimeWireUtils.getHttpServer();
    }

    @Override
    protected void makeRequest(PostMethod request, final ByteArrayCallback callback)
            throws HttpException, IOException {
        exe.execute(request, new HttpClientListener() {

            public boolean requestComplete(HttpMethod method) {
                callback.process(method.getResponseBody());
                exe.releaseResources(method);
                return false;
            }

            public boolean requestFailed(HttpMethod method, IOException exc) {
                exe.releaseResources(method);
                return false;
            }
            
        }, 3000);
    }
}
