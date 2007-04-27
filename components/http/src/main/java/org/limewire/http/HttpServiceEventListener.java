package org.limewire.http;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;

public interface HttpServiceEventListener extends EventListener {

    void responseSent(NHttpConnection conn);

}
