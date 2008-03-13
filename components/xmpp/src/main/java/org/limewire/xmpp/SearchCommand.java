package org.limewire.xmpp;

import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.xmpp.packet.JID;

public class SearchCommand extends Command{
    private final Set<JID> limewireClients;

    SearchCommand(XMPPConnection connection, Set<JID> limewireClients) {
        super(connection);
        this.limewireClients = limewireClients;
    }

    public String getCommand() {
        return "search";
    }

    public void execute(String args) throws Exception {
        for (JID limewireClient : limewireClients) {
            Query query = new Query(args);
            query.setType(IQ.Type.GET);
            query.setTo(limewireClient.toString());
            connection.sendPacket(query);
        }
    }
}
