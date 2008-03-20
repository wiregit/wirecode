package org.limewire.promotion.impressions;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.limewire.promotion.AbstractPromotionBinderRequestor;
import org.limewire.promotion.PromotionBinder;
import org.limewire.promotion.PromotionBinderFactory;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Instances of this will use either return a valid or invalid
 * {@link PromotionBinder}. This flag {@link #isValid} represents a request
 * that has a Binder or one that doesnt. Hence if {@link #isValid} is
 * <code>true</code> we will return a non-null binder; otherwise it will be
 * <code>null</code>.
 */
public class TestPromotionContainerRequestorImpl extends AbstractPromotionBinderRequestor {
    
    private final boolean isValid;

    public TestPromotionContainerRequestorImpl(boolean isValid) {        
        super(new PromotionBinderFactory() {
            public PromotionBinder newBinder(byte[] bytes) {
                //
                // See the contract for this method
                //
                if (bytes == null) {
                    return null;
                }
                return new PromotionBinder(null,null,null);
            }
        });
        this.isValid = isValid;
    }

    public void makeRequest(PostMethod request, ByteArrayCallback callback) throws HttpException, IOException {
        callback.process(isValid ? new byte[]{1,2,3} : null);
    }

    public void error(Exception e) {
        e.printStackTrace();
    }

    public String getUserAgent() {
        return LimeWireUtils.getHttpServer(); // todo
    }

    @Override
    protected String alterUrl(String url) {
        //
        // Don't alter it
        //
        return url;
    }

    public void setNetworkTimeout(int timeoutMillis) {
        // Ignore this, because we never hit the network
    }

}
