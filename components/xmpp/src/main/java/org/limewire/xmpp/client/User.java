package org.limewire.xmpp.client;

import org.jivesoftware.smack.XMPPConnection;

public class User {
    protected String id;
    protected String name;
    protected Presence presence;
    protected XMPPConnection connection;

    public User(String id, String name, Presence presence, XMPPConnection connection) {
        this.id = id;
        this.name = name;
        this.presence = presence;
        this.connection = connection;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Presence getPresence() {
        return presence;
    }
}
