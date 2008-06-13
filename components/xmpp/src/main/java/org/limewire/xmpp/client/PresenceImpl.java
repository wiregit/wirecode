package org.limewire.xmpp.client;

import org.jivesoftware.smack.XMPPConnection;

public class PresenceImpl implements Presence {
    protected final org.jivesoftware.smack.packet.Presence presence;
    protected final XMPPConnection connection;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection) {
        this.presence = presence;
        this.connection = connection;
    }

    public String getJID() {
        return presence.getFrom();
    }

    public Type getType() {
        return Type.valueOf(presence.getType().toString());
    }

    public String getStatus() {
        return presence.getStatus();
    }

    public int getPriority() {
        return presence.getPriority();
    }

    public Mode getMode() {
        return Mode.valueOf(presence.getMode().toString());
    }
}
