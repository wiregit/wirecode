package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.i18n.I18nMarker;

/**
 * Represents xmpp <code><presence></code> messages.
 */
public interface Presence extends FriendPresence {

    enum EventType {PRESENCE_NEW, PRESENCE_UPDATE}

    enum Type {
        available, unavailable, subscribe, subscribed, unsubscribe, unsubscribed, error
    }
    
    /**
     * The actual presence status. 
     */
    enum Mode {
        // lower case enum values to allow direct mapping from to the Mode enum
        // defined in smack
        chat(I18nMarker.marktr("Free to chat")),
        available(I18nMarker.marktr("Available")),
        away(I18nMarker.marktr("Away")),
        xa(I18nMarker.marktr("Away for a while")),
        dnd(I18nMarker.marktr("Do not disturb"));

        private final String name;

        Mode(String name) {
            this.name = name;
        }


        public String toString() {
            return name;
        }
    }
    
    public static final int MIN_PRIORITY = -127;
    
    public static final int MAX_PRIORITY = 127;

    public User getUser();

    /**
     * the jid of the user.  jid's have the form <code>user@host.com/client</code>
     * @return the jid of the user.
     */
    public String getJID();

    /**
     * @return the presence type
     */
    public Type getType();

    /**
     * @return the presence status message; can be <code>null</code>
     */
    public String getStatus();

    /**
     * @return the priority of this presence in relation to other presence's of the same user
     */
    public int getPriority();

    /**
     * @return the presence mode
     */
    public Mode getMode();
}
