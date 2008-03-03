package org.limewire.xmpp;

import java.util.Arrays;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.XMPPConnection;

public class SearchCommand extends Command{
    SearchCommand(XMPPConnection connection) {
        super(connection);
    }

    public String getCommand() {
        return "search";
    }

    public void execute(String args) throws Exception {
        Query query = new Query(args);
        query.setType(IQ.Type.GET);
        query.setTo(connection.getServiceName());  // TODO is service name the right thing to use?
        connection.sendPacket(query);
    }
}
