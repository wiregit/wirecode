package org.limewire.swarm.http.gnutella;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;

public class GHttpUtils {
    
    public static void addFeatures(HttpRequest request, String feature) {
        Header features = request.getFirstHeader(GHttp.FEATURES);
        if(features == null) {
            features = new BasicHeader(GHttp.FEATURES, feature);
        } else {
            request.removeHeader(features);
            features = new BasicHeader(GHttp.FEATURES, features.getValue() + ", " + feature);
        }
        
        request.addHeader(features);
    }

}
