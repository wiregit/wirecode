package org.limewire.xmpp.client;

public class PresenceImpl implements Presence {
    private final org.jivesoftware.smack.packet.Presence presence;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence) {
        this.presence = presence;
    }

    public String getJID() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
