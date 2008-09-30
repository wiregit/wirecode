package org.limewire.xmpp.api.client;

import org.limewire.i18n.I18nMarker;

/**
 * Represents xmpp <code><presence></code> messages.
 */
public interface Presence {
    
    enum Type {
        available, unavailable, subscribe, subscribed, unsubscribe, unsubscribed, error
    }
    
    enum Mode {
        chat(I18nMarker.marktr("Chatting")),
        available(I18nMarker.marktr("Available")),
        away(I18nMarker.marktr("Idle")),
        xa(I18nMarker.marktr("Idle")),
        dnd(I18nMarker.marktr("Away"));

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

    /**
     * Used by the xmpp service user to initiate a new chat
     * @param reader the <code>MessageReader</code> to be used to process incoming
     * messages
     * @return the <code>MessageWriter</code> used to send outgoing messages
     */
    public MessageWriter createChat(MessageReader reader);

    /**
     * Used by the xmpp service user to register a listener for new incoming chats
     * @param listener the <code>IncomingChatListener</code> to be used
     */
    public void setIncomingChatListener(IncomingChatListener listener);
}
