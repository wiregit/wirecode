package org.limewire.xmpp.client.commands;

import org.jivesoftware.smack.XMPPConnection;

public class InitiateChatCommand extends Command {
    public InitiateChatCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "chat";
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
