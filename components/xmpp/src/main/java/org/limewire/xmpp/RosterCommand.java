package org.limewire.xmpp;

import org.jivesoftware.smack.XMPPConnection;

public class RosterCommand extends Command {
    RosterCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "roster";
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
