package org.limewire.xmpp.client.commands;

import org.jivesoftware.smack.XMPPConnection;

public class SendMessageCommand extends Command {
    public SendMessageCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return DEFAULT;
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
