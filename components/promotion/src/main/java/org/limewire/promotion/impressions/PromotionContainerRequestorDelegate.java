/**
 * 
 */
package org.limewire.promotion.impressions;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;

public interface PromotionContainerRequestorDelegate {

    void error(Exception e);

    String getUserAgent();
    
    void makeRequest(PostMethod request, ByteArrayCallback callback) throws HttpException, IOException;
}