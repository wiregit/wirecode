package org.limewire.swarm.http.gnutella.interceptor;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.limewire.swarm.http.gnutella.GHttp;
import org.limewire.swarm.http.gnutella.GHttpUtils;

public class SupportedFeaturesInterceptor implements HttpRequestInterceptor {
    
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if(!request.containsHeader(GHttp.QUEUE))
            request.addHeader(new BasicHeader(GHttp.QUEUE, "0.1"));
        GHttpUtils.addFeatures(request, GHttp.FEATURE_QUEUE+"/0.1");
    }

}
