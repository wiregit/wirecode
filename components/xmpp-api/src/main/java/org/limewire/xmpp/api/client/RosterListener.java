package org.limewire.xmpp.api.client;

import com.google.inject.Inject;

/**
 * Allows users of the xmpp service to listen for chages to their roster.
 * Note that user additions / deletions do not correpsond to those users
 * being online / offline (i.e., presence available / unavailable).
 *
 * The roster is initially populated via <code>userAdded</code> when a user signs on
 * during <code>XMPPService.start()</code>
 */
public interface RosterListener {
    @Inject 
    public void register(XMPPService xmppService);
    
    public void userAdded(User user);
    
    public void userUpdated(User user);
    
    public void userDeleted(String id);
}
