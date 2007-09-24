package com.limegroup.gnutella.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.settings.ChatSettings;

@Singleton
public class FeaturesWriter {
    
    private final NetworkManager networkManager;
    
    @Inject
    public FeaturesWriter(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Utility method for writing the currently supported features
     * to the <tt>Writer</tt>.
     */
    public void writeFeatures(Writer writer) throws IOException {
        Set<HTTPHeaderValue> features = getFeaturesValue();
        // Write X-Features header.h
        if (features.size() > 0) {
            HTTPUtils.writeHeader(HTTPHeaderName.FEATURES,
                    new HTTPHeaderValueCollection(features), writer);
        }
    }

    /**
     * Utility method for writing the currently supported features
     * to the <tt>OutputStream</tt>.
     */
    public void writeFeatures(OutputStream stream) throws IOException {
        Set<HTTPHeaderValue> features = getFeaturesValue();
        // Write X-Features header.
        if (features.size() > 0) {
            HTTPUtils.writeHeader(HTTPHeaderName.FEATURES,
                    new HTTPHeaderValueCollection(features), stream);
        }
    }

    /**
     * Utlity method for getting the currently supported features.
     */
    public Set<HTTPHeaderValue> getFeaturesValue() {
        Set<HTTPHeaderValue> features = new HashSet<HTTPHeaderValue>(4);
        features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
        if (ChatSettings.CHAT_ENABLED.getValue())
            features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
        
       	features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
       	
       	if (!networkManager.acceptedIncomingConnection() && networkManager.canDoFWT())
       	    features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        
        return features;
    }

}
