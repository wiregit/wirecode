package org.limewire.xmpp;

import org.jivesoftware.smack.XMPPConnection;

public class BrowseCommand extends Command {
    BrowseCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "browse";
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
