package org.limewire.xmpp.client;

import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {
    
    private CopyOnWriteArrayList<LibraryListener> libraryListeners;
    private Library library;

    LimePresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection) {
        super(presence, connection);
        this.libraryListeners = new CopyOnWriteArrayList<LibraryListener>();
    }
    
    public Library getLibrary() {
        return library;
    }
    
    void setLibrary(Library library) {
        this.library = library;
        fireLibraryListeners();
    }

    private void fireLibraryListeners() {
        for(LibraryListener libraryListener : libraryListeners) {
            libraryListener.libraryAdded(library);
        }
    }

    void sendGetLibrary() {
        Library libraryIQ = new Library();
        libraryIQ.setType(IQ.Type.GET);
        libraryIQ.setTo(getJID());
        libraryIQ.setPacketID(IQ.nextID());
        connection.sendPacket(libraryIQ);
        final PacketCollector collector = connection.createPacketCollector(
            new PacketIDFilter(libraryIQ.getPacketID()));
        connection.sendPacket(libraryIQ);
        Thread responseThread = new Thread(new Runnable() {
            public void run() {
                Library response = (Library) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
                collector.cancel();
                setLibrary(response);
            }
        });
        responseThread.setDaemon(true);
        responseThread.start();
    }
    
    public void addLibraryListener(LibraryListener libraryListener) {
        libraryListeners.add(libraryListener);
        // TODO fire exiting library
    }
    
    public void sendFile(java.io.File file) {
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new FileContentHandler(file, true);
            OutgoingJingleSession out = manager.createOutgoingJingleSession(getJID(), fileContentHandler);

            out.start();

            while (out.getJingleMediaSession() == null) {
                Thread.sleep(500);
            }

            //out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    
}
