package org.limewire.xmpp.client;

public interface RosterListener {
    void userAdded(User user);
    
    void userUpdated(User user);
    
    void userDeleted(String id);
}
