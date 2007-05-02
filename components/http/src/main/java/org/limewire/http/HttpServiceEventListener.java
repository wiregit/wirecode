package org.limewire.http;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;

public interface HttpServiceEventListener extends EventListener {

    void requestReceived(NHttpConnection conn);
    
    void responseSent(NHttpConnection conn);

}
