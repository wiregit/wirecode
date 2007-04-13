package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpServerConnection;

public interface ServerConnectionEventListener {

    void fatalIOException(NHttpServerConnection conn, IOException ex);

    void fatalProtocolException(NHttpServerConnection conn, HttpException ex);

    void connectionOpen(NHttpServerConnection conn);

    void connectionClosed(NHttpServerConnection conn);

    void connectionTimeout(NHttpServerConnection conn);

    void responseBodySent(NHttpServerConnection conn, HttpResponse httpResponse);

    void responseHeadersSent(NHttpServerConnection conn, HttpResponse response);

}
