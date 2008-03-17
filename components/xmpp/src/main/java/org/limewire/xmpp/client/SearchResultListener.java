package org.limewire.xmpp.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ;

public class SearchResultListener implements PacketListener {
    public void processPacket(Packet packet) {
        SearchResult result = (SearchResult)packet;
        if(result.getType().equals(IQ.Type.RESULT)) {
            handleResult(result );
        }
    }

    private void handleResult(SearchResult result) {
        //IQ queryResult = new SearchResult();
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof SearchResult;
                //return packet.getExtension("search", "jabber:iq:lw-search") != null;
            }
        };
    }
}
