package org.limewire.xmpp.client.commands;

import org.jivesoftware.smack.XMPPConnection;

public abstract class Command {
    public static final String DEFAULT = "";
    
    protected XMPPConnection connection;
    
    Command(XMPPConnection connection) {
        this.connection = connection;
    }
    
    public abstract String getCommand();
    public abstract void execute(String args) throws Exception;
}
