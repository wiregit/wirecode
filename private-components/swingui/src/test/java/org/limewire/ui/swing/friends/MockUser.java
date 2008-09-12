package org.limewire.ui.swing.friends;

import java.util.Map;

import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence;

public class MockUser implements User {
    private String id;
    private String name;
    
    public MockUser(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public void addPresenceListener(PresenceListener presenceListener) {
        // TODO Auto-generated method stub

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
    public boolean jidBelongsTo(String jid) {
        // TODO Auto-generated method stub
        return false;
    }

    public Map<String, Presence> getPresences() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
