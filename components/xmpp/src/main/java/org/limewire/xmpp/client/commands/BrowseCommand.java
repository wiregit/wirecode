package org.limewire.xmpp.client.commands;

import org.jivesoftware.smack.XMPPConnection;

public class BrowseCommand extends Command {
    public BrowseCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "browse";
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
