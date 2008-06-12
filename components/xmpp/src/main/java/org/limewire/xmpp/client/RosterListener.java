package org.limewire.xmpp.client;

public interface RosterListener {
    void userAdded(User user);
    
    void userUpdated(User user);
    
    void userDeleted(User user);
    
    void presenceChanged(User user);
}
