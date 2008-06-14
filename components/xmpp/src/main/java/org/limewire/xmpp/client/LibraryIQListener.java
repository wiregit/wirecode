package org.limewire.xmpp.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

public class LibraryIQListener implements PacketListener {
    private XMPPConnection connection;
    private File[] files;

    public LibraryIQListener(XMPPConnection connection, File[] files) {
        this.files = files;
        this.connection = connection;
    }
    
    public void setFiles(File[] files) {
        this.files = files;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public void processPacket(Packet packet) {
        LibraryIQ iq = (LibraryIQ)packet;
        if(iq.getType().equals(IQ.Type.GET)) {
            handleGet(iq);
        } else if(iq.getType().equals(IQ.Type.RESULT)) {
            handleResult(iq);
        } else if(iq.getType().equals(IQ.Type.SET)) {
            //handleSet(iq);
        } else if(iq.getType().equals(IQ.Type.ERROR)) {
            //handleError(iq);
        } else {
            //sendError(packet);
        }
    }

    private void handleResult(LibraryIQ libraryIQ) {

    }

    private void handleGet(LibraryIQ packet) {
        LibraryIQ queryResult = new LibraryIQ(files);
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(packet.getPacketID());
        queryResult.setType(IQ.Type.RESULT);
        connection.sendPacket(queryResult);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof LibraryIQ;
            }
        };
    }
}
