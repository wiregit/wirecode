package com.limegroup.gnutella.version;

import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UpdateCollectionFactoryImpl implements UpdateCollectionFactory {

    private final HttpClientInstanceUtils httpClientInstanceUtils;

    @Inject
    public UpdateCollectionFactoryImpl(HttpClientInstanceUtils httpClientInstanceUtils) {
        this.httpClientInstanceUtils = httpClientInstanceUtils;
    
    }
    
    public UpdateCollection createUpdateCollection(String xml) throws InvalidDataException {
        return new UpdateCollectionImpl(xml, httpClientInstanceUtils);
    }

}
