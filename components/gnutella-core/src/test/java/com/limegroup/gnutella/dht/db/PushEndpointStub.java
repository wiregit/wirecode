package com.limegroup.gnutella.dht.db;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.AbstractPushEndpoint;
import com.limegroup.gnutella.PushEndpoint;

class PushEndpointStub extends AbstractPushEndpoint {

    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public PushEndpoint createClone() {
        return this;
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public byte[] getClientGUID() {
        return null;
    }

    @Override
    public byte getFeatures() {
        return 0;
    }

    @Override
    public int getFWTVersion() {
        return 0;
    }

    @Override
    public Set<? extends IpPort> getProxies() {
        return null;
    }

    @Override
    public IpPort getValidExternalAddress() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public void updateProxies(boolean good) {
    }

    @Override
    public String getAddressDescription() {
        return null;
    }
}