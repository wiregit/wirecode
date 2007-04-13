package org.limewire.http;

import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpServerConnection;

public interface HttpResponseListener {

    void responseSent(NHttpServerConnection conn, HttpResponse response);

    void connectionClosed(NHttpServerConnection conn);

    void connectionOpened(NHttpServerConnection conn);
    
}
