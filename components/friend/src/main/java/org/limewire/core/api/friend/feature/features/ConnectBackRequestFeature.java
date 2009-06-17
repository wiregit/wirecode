package org.limewire.core.api.friend.feature.features;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.friend.feature.Feature;
import org.limewire.net.ConnectBackRequest;

public class ConnectBackRequestFeature extends Feature<ConnectBackRequest> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/connect-back-request/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public ConnectBackRequestFeature() {
        super(ID);
    }
}
