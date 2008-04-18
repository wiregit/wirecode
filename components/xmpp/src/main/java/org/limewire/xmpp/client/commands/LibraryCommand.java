package org.limewire.xmpp.client.commands;

import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.xmpp.client.Library;

public class LibraryCommand extends Command {
    private final Set<String> limewireClients;

    public LibraryCommand(XMPPConnection connection, Set<String> limewireClients) {
        super(connection);
        this.limewireClients = limewireClients;
    }

    public String getCommand() {
        return "library";
    }

    public void execute(String args) throws Exception {
        for (String limewireClient : limewireClients) {
            System.out.println("get library of " + limewireClient);
            Library query = new Library();
            query.setType(IQ.Type.GET);
            query.setTo(limewireClient);
            query.setPacketID(IQ.nextID());
            connection.sendPacket(query);
        }
    }
}
