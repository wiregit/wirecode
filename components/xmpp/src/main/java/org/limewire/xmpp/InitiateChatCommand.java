package org.limewire.xmpp;

import org.jivesoftware.smack.XMPPConnection;

public class InitiateChatCommand extends Command {
    InitiateChatCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "chat";
    }

    public void execute(String args) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
