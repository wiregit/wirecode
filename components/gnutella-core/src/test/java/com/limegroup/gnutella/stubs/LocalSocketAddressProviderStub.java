package com.limegroup.gnutella.stubs;

import org.limewire.io.LocalSocketAddressProvider;

public class LocalSocketAddressProviderStub implements LocalSocketAddressProvider {

    private byte[] localAddress;
    private int localPort;
    private boolean localAddressPrivate;
    private boolean tlsCapable;
    
    public byte[] getLocalAddress() {
        return localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public boolean isLocalAddressPrivate() {
        return localAddressPrivate;
    }

    public void setLocalAddress(byte[] localAddress) {
        this.localAddress = localAddress;
    }

    public void setLocalAddressPrivate(boolean localAddressPrivate) {
        this.localAddressPrivate = localAddressPrivate;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }
    
    public boolean isTLSCapable() {
        return tlsCapable;
    }
    
    public void setTLSCapable(boolean tlsCapable) {
        this.tlsCapable = tlsCapable;
    }

}