package org.limewire.xmpp.client;

import org.jivesoftware.smack.XMPPConnection;

public class User {
    protected String jid;
    protected String name;
    protected XMPPConnection connection;

    public User(String jid, String name, XMPPConnection connection) {
        this.jid = jid;
        this.name = name;
        this.connection = connection;
    }

    public String getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }
}
