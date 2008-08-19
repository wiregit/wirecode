package org.limewire.swarm.http;

import org.limewire.swarm.SwarmSource;

/**
 * Container class to attach SwarmSource to selection key.
 * 
 */
class HttpSourceAttachment {
    public final SwarmSource source;

    public HttpSourceAttachment(SwarmSource source) {
        this.source = source;
    }

    public SwarmSource getSource() {
        return source;
    }
}