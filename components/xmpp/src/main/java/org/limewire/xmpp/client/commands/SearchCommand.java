package org.limewire.xmpp.client.commands;

import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.xmpp.client.Search;

public class SearchCommand extends Command{
    private final Set<String> limewireClients;

    public SearchCommand(XMPPConnection connection, Set<String> limewireClients) {
        super(connection);
        this.limewireClients = limewireClients;
    }

    public String getCommand() {
        return "search";
    }

    public void execute(String args) throws Exception {
        String id = IQ.nextID();
        for (String limewireClient : limewireClients) {
            System.out.println("searching " + limewireClient + " for " + args);
            Search query = new Search(args);
            query.setType(IQ.Type.GET);
            query.setTo(limewireClient);
            query.setPacketID(id);
            connection.sendPacket(query);
        }
    }
}
