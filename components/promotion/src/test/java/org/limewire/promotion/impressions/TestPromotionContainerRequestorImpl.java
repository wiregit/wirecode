package org.limewire.promotion.impressions;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.limewire.promotion.PromotionBinder;
import org.limewire.promotion.PromotionBinderFactory;
import org.limewire.promotion.AbstractPromotionBinderRequestor;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Instances of this will use the httpclient classes directly to execute HTTP
 * requests.
 */
public class TestPromotionContainerRequestorImpl extends AbstractPromotionBinderRequestor {

    public TestPromotionContainerRequestorImpl() {
        super(new PromotionBinderFactory() {
            public PromotionBinder newBinder(byte[] bytes) {
                return new PromotionBinder(null,null,null);
            }
        });
    }

    public void makeRequest(PostMethod request, ByteArrayCallback callback) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        int ret = client.executeMethod(request);

        if (ret == HttpStatus.SC_NOT_IMPLEMENTED) {
            System.err.println("The Post method is not implemented by this URI");
            // still consume the response body
            request.getResponseBodyAsString();
        } else {
            callback.process(request.getResponseBody());
        }
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

}
