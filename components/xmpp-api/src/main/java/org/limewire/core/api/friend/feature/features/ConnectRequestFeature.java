package org.limewire.core.api.friend.feature.features;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.friend.feature.Feature;

public class ConnectRequestFeature extends Feature<String> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/connect-request/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public ConnectRequestFeature() {
        super(ID.toASCIIString(), ID);
    }
}
