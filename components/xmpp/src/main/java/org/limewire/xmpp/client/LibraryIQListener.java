package org.limewire.xmpp.client;

import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

public class LibraryIQListener implements PacketListener {
    private XMPPConnection connection;
    private final LibrarySource librarySource;
    private ConcurrentHashMap <String, LibraryListener> libraryHandlers = new ConcurrentHashMap<String, LibraryListener>();

    public LibraryIQListener(XMPPConnection connection, LibrarySource librarySource) {
        this.librarySource = librarySource;
        this.connection = connection;
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
        System.out.println("handling library result..." + libraryIQ.getPacketID());
        LibraryListener listener = libraryHandlers.get(libraryIQ.getPacketID());
        if(listener != null) {
            System.out.println("notifying listener: " + listener);
            for(File f : libraryIQ.getFiles()) {
                listener.fileAdded(f);
            }
            libraryHandlers.remove(libraryIQ.getPacketID());
        }
    }
    
    public void addLibraryListener(LibraryIQ request, LibraryListener listener) {
        libraryHandlers.put(request.getPacketID(), listener);
    }

    private void handleGet(LibraryIQ packet) {
        LibraryIQ queryResult = new LibraryIQ(librarySource);
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
