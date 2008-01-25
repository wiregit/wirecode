package org.limewire.net;

import java.net.InetAddress;

/**
 * Allows a <code>Socket</code> user to specify the local port to bind to
 */
public class PerCallSocketBindingSettingsImpl implements PerCallSocketBindingSettings {
    private final InetAddress address;
    private int port;

    public PerCallSocketBindingSettingsImpl(InetAddress address, int port) {
        if(port < 0) {
            throw new IllegalArgumentException("negative ports not allowed: " + port);
        }
        this.address = address;
        this.port = port;
    }
    
    public boolean isSocketBindingRequired() {
        return (address != null || port > 0);
    }

    public String getAddressToBindTo() {
        return address != null ? address.getHostAddress() : null;
    }

    public int getPortToBindTo() {
        return port;
    }

    public void bindingFailed() {
        // TODO throw exception?
    }
}
