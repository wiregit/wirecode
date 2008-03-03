package org.limewire.xmpp;

import org.jivesoftware.smack.XMPPConnection;

public class SendMessageCommand extends Command {
    SendMessageCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return DEFAULT;
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
