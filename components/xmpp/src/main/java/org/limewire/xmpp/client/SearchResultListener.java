package org.limewire.xmpp.client;

import org.jivesoftware.smack.PacketListener;
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
        //To change body of created methods use File | Settings | File Templates.
    }
}
