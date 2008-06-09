package org.limewire.swarm.http;

import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;

public interface SwarmNHttpRequestExecutionHandler<E> extends NHttpRequestExecutionHandler {
    
    /**
     * Returns an object that should be used when calling
     * {@link ConnectingIOReactor#connect(java.net.SocketAddress, java.net.SocketAddress, Object, org.apache.http.nio.reactor.SessionRequestCallback)}
     * as the attachment.
     * 
     * This will ensure that the ExecutionHandler can callback correctly on the HttpSwarmHandler. 
     */
    public Object createAttachment(HttpSwarmHandler<E> swarmHandler, E object);

}
