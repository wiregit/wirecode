package org.limewire.xmpp.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.dom4j.Element;

import java.io.File;
import java.util.List;

public class SearchListener implements PacketListener {
    private final List<File> sharedResources;

    public SearchListener(List<File> sharedResources) {
        this.sharedResources = sharedResources;
    }

    public void processPacket(Packet packet) {
        IQ iq = (IQ)packet;
        if(iq.getType().equals(IQ.Type.GET)) {
            handleGet(iq);
        } else if(iq.getType().equals(IQ.Type.RESULT)) {
            handleResult(iq);
        } else if(iq.getType().equals(IQ.Type.SET)) {
            handleSet(iq);
        } else if(iq.getType().equals(IQ.Type.ERROR)) {
            handleError(iq);
        } else {
            //sendError(packet);
        }
    }

    private void handleError(IQ packet) {
        System.out.println("ERROR:\n" + packet.toXML());
    }

    private void handleSet(IQ packet) {
        // sendError(packet);
    }

    private void handleGet(IQ packet) {
        IQ queryResult = new SearchResult();
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(IQ.nextID());

    }

    public static class SearchFilter implements PacketFilter {
        public boolean accept(Packet packet) {
            return packet.getExtension("search", "jabber:iq:lw-search") != null;
        }
    }
}
