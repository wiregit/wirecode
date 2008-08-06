package org.limewire.swarm.http;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;

class HttpSourceInfo {
    public final SwarmSource source;

    public final SwarmSourceEventListener sourceEventListener;

    public HttpSourceInfo(SwarmSource source, SwarmSourceEventListener sourceEventListener) {
        this.source = source;
        this.sourceEventListener = sourceEventListener;
    }

    public SwarmSource getSource() {
        return source;
    }

    public SwarmSourceEventListener getSourceEventListener() {
        return sourceEventListener;
    }
}