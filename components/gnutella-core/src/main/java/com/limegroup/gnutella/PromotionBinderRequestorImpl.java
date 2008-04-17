package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpParams;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.promotion.AbstractPromotionBinderRequestor;
import org.limewire.promotion.PromotionBinderFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class PromotionBinderRequestorImpl extends AbstractPromotionBinderRequestor {
    
    private final static Log LOG = LogFactory.getLog(PromotionBinderRequestorImpl.class);
    
    private final LimeHttpClient exe;
    private final ApplicationServices applicationServices;
    
    @Inject
    public PromotionBinderRequestorImpl(PromotionBinderFactory binderFactory, LimeHttpClient exe, ApplicationServices applicationServices) {
        super(binderFactory);
        this.exe = exe;
        this.applicationServices = applicationServices;
    }
 
    protected void error(Exception e) {e.printStackTrace();
        LOG.error("Error processing promotion binder", e);
    }

    protected String getUserAgent() {
        return LimeWireUtils.getHttpServer();
    }

    protected InputStream makeRequest(final HttpPost post, HttpParams params)throws HttpException, IOException {
        HttpResponse response = exe.execute(post);
        return response.getEntity().getContent();
    }
    
    public String alterUrl(String url) {
        return LimeWireUtils.addLWInfoToUrl(url, applicationServices.getMyBTGUID());
    } 
}
