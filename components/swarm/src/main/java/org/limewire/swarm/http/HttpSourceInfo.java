package org.limewire.swarm.http;

import org.limewire.swarm.SourceEventListener;
import org.limewire.swarm.SwarmSource;

public class HttpSourceInfo {
    public final SwarmSource source;

    public final SourceEventListener sourceEventListener;

    public HttpSourceInfo(SwarmSource source, SourceEventListener sourceEventListener) {
        this.source = source;
        this.sourceEventListener = sourceEventListener;
    }

    public SwarmSource getSource() {
        return source;
    }

    public SourceEventListener getSourceEventListener() {
        return sourceEventListener;
    }
}