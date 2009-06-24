package org.limewire.xmpp.client.impl;

import java.util.List;
import java.util.Collections;

import org.limewire.listener.EventListener;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.RosterEvent;
import org.limewire.io.UnresolvedIpPort;

public class FriendConnectionConfigurationMock implements FriendConnectionConfiguration {
    private final String username;
    private final String password;
    private final String serviceName;
    private final EventListener<RosterEvent> rosterListener;

    public FriendConnectionConfigurationMock(String username, String password,
            String serviceName, EventListener<RosterEvent> rosterListener) {
        this.username = username;
        this.password = password;
        this.serviceName = serviceName;
        this.rosterListener = rosterListener;
    }
    
    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public String getUserInputLocalID() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String getLabel() {
        return serviceName;
    }

    @Override
    public String getCanonicalizedLocalID() {
        return getUserInputLocalID();
    }

    @Override
    public String getNetworkName() {
        return getServiceName();
    }
    
    @Override
    public Type getType() {
        return Type.XMPP;
    }

    @Override
    public String getResource() {
        return "LimeWire";
    }
    
    @Override
    public EventListener<RosterEvent> getRosterListener() {
        return rosterListener;
    }

    @Override public List<UnresolvedIpPort> getDefaultServers() {
        return Collections.emptyList();
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public void setAttribute(String key, Object property) {
    }
}