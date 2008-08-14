package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.User;

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
}
