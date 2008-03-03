package org.limewire.xmpp;

import org.jivesoftware.smack.XMPPConnection;

public class DownloadCommand extends Command {
    DownloadCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "get";
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
