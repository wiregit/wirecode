package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ContentListener;

interface HttpSwarmHandler<E> {

    /** Request for a new ContentListener for the given Response. */
    ContentListener getContentListener(HttpResponse response, E attachment)
            throws IOException;

    /** Request for a new outgoing HTTP Request. */
    HttpRequest getHttpRequest(E attachment) throws IOException;

    /** Notification that the connection is closed and should be cleaned up. */
    void cleanup(E attachment);

    /** Notification that a response has succesfully been processed. */
    void responseProcessed(HttpResponse response, E attachment);

    /** Notification that a connection has been succesfully established. */
    void connectionEstablished(E attachment);

}
