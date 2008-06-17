package org.limewire.xmpp.client;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {
    
    private LibraryListener libraryListener;
    protected LibraryIQListener IQListener;

    LimePresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection, LibraryIQListener libraryIQListener) {
        super(presence, connection);
        this.IQListener = libraryIQListener;
    }
    
    void sendGetLibrary() {
        final LibraryIQ libraryIQ = new LibraryIQ();
        libraryIQ.setType(IQ.Type.GET);
        libraryIQ.setTo(getJID());
        libraryIQ.setPacketID(IQ.nextID());
        IQListener.addLibraryListener(libraryIQ, libraryListener);
        //final PacketCollector collector = connection.createPacketCollector(
        //    new PacketIDFilter(libraryIQ.getPacketID()));         
        //Thread responseThread = new Thread(new Runnable() {
        //    public void run() {
                connection.sendPacket(libraryIQ);
                //LibraryIQ response = (LibraryIQ) collector.nextResult();
                //collector.cancel();
                //response.parseFiles(libraryListener);
        //    }
        //});
        //responseThread.setDaemon(true);
        //responseThread.start();
    }
    
    public void setLibraryListener(LibraryListener libraryListener) {
        this.libraryListener = libraryListener;
        // TODO fire exiting library
    }

    public void requestFile(File file) {
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new FileContentHandler(new java.io.File(""), false);
            OutgoingJingleSession out = manager.createOutgoingJingleSession(getJID(), fileContentHandler);

            out.start();

            while (out.getJingleMediaSession() == null) {
                Thread.sleep(500);
            }

            out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

            out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    
}
