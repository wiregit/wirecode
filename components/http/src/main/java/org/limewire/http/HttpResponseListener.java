package org.limewire.http;

import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;

public interface HttpResponseListener {

    void connectionClosed(NHttpConnection conn);

    void connectionOpen(NHttpConnection conn);

    void responseSent(NHttpConnection conn, HttpResponse response);
    
}
