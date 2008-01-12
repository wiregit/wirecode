package org.limewire.net;

import java.net.InetAddress;

import org.limewire.io.NetworkUtils;

public class SocketBindingSettingsImpl implements SocketBindingSettings{
    private final InetAddress address;
    private int port;

    public SocketBindingSettingsImpl(InetAddress address, int port) {
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
        if(port < 0) {
            port = 0;
        }
        return port;
    }

    public void bindingFailed() {
        // TODO throw exception?
    }
}
