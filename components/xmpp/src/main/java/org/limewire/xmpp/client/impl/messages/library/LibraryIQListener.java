package org.limewire.xmpp.client.impl.messages.library;

import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.LibraryListener;
import org.limewire.xmpp.client.service.LibraryProvider;

/**
 * Handles <code><library></code> messages.  Sends <code>result</code> messages when <code>get</code>
 * messages are received.  Notifies the proper <code>LibraryListener</code> when <code>result</code>
 * messages are received.
 */
public class LibraryIQListener implements PacketListener {

    private static final Log LOG = LogFactory.getLog(LibraryIQListener.class);

    private final XMPPConnection connection;
    private final LibraryProvider libraryProvider;
    private final ConcurrentHashMap <String, LibraryListener> libraryHandlers = new ConcurrentHashMap<String, LibraryListener>();

    public LibraryIQListener(XMPPConnection connection, LibraryProvider libraryProvider) {
        this.libraryProvider = libraryProvider;
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
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling library result " + libraryIQ.getPacketID());
        }
        LibraryListener listener = libraryHandlers.get(libraryIQ.getPacketID());
        if(listener != null) {
            for(FileMetaData f : libraryIQ.getFiles()) {
                listener.fileAdded(f);
            }
            libraryHandlers.remove(libraryIQ.getPacketID());
        }
    }
    
    public void addLibraryListener(LibraryIQ request, LibraryListener listener) {
        libraryHandlers.put(request.getPacketID(), listener);
    }

    private void handleGet(LibraryIQ packet) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling library get " + packet.getPacketID());
        }
        LibraryIQ queryResult = new LibraryIQ(libraryProvider);
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
