package org.limewire.swarm;

import java.io.IOException;
import java.util.List;

public interface SwarmSourceHandler {

    void addSource(SwarmSource source);

    List<SwarmSource> getSources();

    void start() throws IOException;

    void shutdown() throws IOException;

    boolean isActive();

    boolean isComplete();

    float getMeasuredBandwidth(boolean downstream);

    boolean hasSource(SwarmSource source);

    boolean isBadSource(SwarmSource source);
}
