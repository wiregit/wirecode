package com.limegroup.gnutella;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.limewire.promotion.AbstractPromotionBinderRequestor;
import org.limewire.promotion.PromotionBinderFactory;
import org.limewire.promotion.impressions.InputStreamCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class PromotionBinderRequestorImpl extends AbstractPromotionBinderRequestor {
    
    private final static Log LOG = LogFactory.getLog(PromotionBinderRequestorImpl.class);
    
    private final HttpExecutor exe;
    private final ApplicationServices applicationServices;
    
    /** The default timeout is 5000, though we can change that. */
    private int timeoutMillis = 5000;
    
    @Inject
    public PromotionBinderRequestorImpl(PromotionBinderFactory binderFactory, HttpExecutor exe, ApplicationServices applicationServices) {
        super(binderFactory);
        this.exe = exe;
        this.applicationServices = applicationServices;
    }
    
    public void setNetworkTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }    

    @Override
    protected void error(Exception e) {
        LOG.error("Error processing promotion binder", e);
    }

    @Override
    protected String getUserAgent() {
        return LimeWireUtils.getHttpServer();
    }

    //@Override
    protected void makeRequest(final HttpPost post, HttpParams params, final InputStreamCallback callback)
            throws HttpException, IOException {
        exe.execute(post, params, new HttpClientListener() {

            public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
                try {
                    callback.process(response.getEntity().getContent());
                } catch (IllegalStateException e) {
                    LOG.error(e);
                } catch (IOException e) {
                    LOG.error(e);
                } finally {
                    exe.releaseResources(response);
                }
                return false;
            }

            public boolean requestFailed(HttpUriRequest request, HttpResponse response,
                    IOException exc) {
                callback.process(null);
                exe.releaseResources(response);
                return false;
            }
            
        });
    }

    @Override
    protected String alterUrl(String url) {
        return LimeWireUtils.addLWInfoToUrl(url, applicationServices.getMyBTGUID());
    }   
}
