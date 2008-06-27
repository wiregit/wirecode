package org.limewire.xmpp.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;
import org.jivesoftware.smackx.jingle.file.FileLocator;
import org.jivesoftware.smackx.jingle.file.InitiatorFileContentHandler;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {

    private static final Log LOG = LogFactory.getLog(LimePresenceImpl.class);
    
    private LibraryListener libraryListener;
    protected LibraryIQListener IQListener;
    private final FileLocator fileLocator;

    LimePresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection, LibraryIQListener libraryIQListener, FileLocator fileLocator) {
        super(presence, connection);
        this.IQListener = libraryIQListener;
        this.fileLocator = fileLocator;
    }
    
    void sendGetLibrary() {
        LOG.info("getting library from " + getJID() + "...");
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

    public void requestFile(FileMetaData file, FileTransferProgressListener progressListener) {
        LOG.info("requesting file " + file.toString() + " from " + getJID());
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new InitiatorFileContentHandler(new FileMetaDataAdapter(file), false, fileLocator);
            fileContentHandler.setProgressListener(new ProgressListenerAdapter(progressListener));
            OutgoingJingleSession out = manager.createOutgoingJingleSession(getJID(), fileContentHandler);

            out.start();
        } catch (XMPPException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void sendFile(FileMetaData file, FileTransferProgressListener progressListener) {
        LOG.info("sending file " + file.toString() + " to " + getJID());
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new InitiatorFileContentHandler(new FileMetaDataAdapter(file), true, fileLocator);
            fileContentHandler.setProgressListener(new ProgressListenerAdapter(progressListener));
            OutgoingJingleSession out = manager.createOutgoingJingleSession(getJID(), fileContentHandler);

            out.start();
        } catch (XMPPException e) {
            LOG.error(e.getMessage(), e);
        }
    }
    
    
}
