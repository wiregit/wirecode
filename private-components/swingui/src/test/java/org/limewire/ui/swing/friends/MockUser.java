package org.limewire.ui.swing.friends;

import java.util.HashMap;
import java.util.Map;

import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.core.api.friend.Network;
import org.limewire.listener.EventListener;

public class MockUser implements User {
    private String id;
    private String name;
    
    public MockUser(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        // TODO Auto-generated method stub

    }
    
    @Override
    public String getRenderName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Presence> getPresences() {
        return new HashMap<String, Presence>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public Network getNetwork() {
        return new Network() {
            public String getMyID() {
                return "";
            }

            public String getNetworkName() {
                return "mock-network";
            }
        };
    }
}
