package com.limegroup.mojito.event.exceptions;

import java.net.SocketAddress;
import java.util.Set;

public class BootstrapTimeoutException extends Exception {
    
    private Set<SocketAddress> failedHosts;
    
    public BootstrapTimeoutException(Set<SocketAddress> failedHosts) {
        this.failedHosts = failedHosts;
    }

    public Set<SocketAddress> getFailedHosts() {
        return failedHosts;
    }
}
