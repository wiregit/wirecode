package org.limewire.swarm;

public interface SwarmIOControl {
    public void requestInput();

    public void suspendInput();

    public void requestOutput();

    public void suspendOutput();

    public void shutdown() throws java.io.IOException;
}
