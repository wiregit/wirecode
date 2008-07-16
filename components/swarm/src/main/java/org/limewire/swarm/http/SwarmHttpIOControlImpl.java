package org.limewire.swarm.http;

import java.io.IOException;

import org.limewire.swarm.SwarmIOControl;


public class SwarmHttpIOControlImpl implements SwarmIOControl {
    private final org.apache.http.nio.IOControl ioControl;

    public SwarmHttpIOControlImpl(org.apache.http.nio.IOControl ioControl) {
        assert ioControl != null;
        this.ioControl = ioControl;
    }

    public void requestInput() {
        ioControl.requestInput();
    }

    public void requestOutput() {
        ioControl.requestOutput();
    }

    public void shutdown() throws IOException {
        ioControl.shutdown();
    }

    public void suspendInput() {
        ioControl.suspendInput();
    }

    public void suspendOutput() {
        ioControl.suspendOutput();
    }
}
