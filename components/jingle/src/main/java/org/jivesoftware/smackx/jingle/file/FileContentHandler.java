package org.jivesoftware.smackx.jingle.file;

import java.io.File;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.file.FileDescription;


public class FileContentHandler extends JingleContentHandler {
    
    public enum ExchangeType { Request, Offer }
    
    private File file;
    private boolean sending;
    private UserAcceptor userAcceptor;
    private File saveDir;
    private FileTransferProgressListener progressListener;

    public FileContentHandler(File file, boolean sending, File saveDir) {
        this(file, sending, getAlwaysAcceptor(), saveDir);
    }

    private static UserAcceptor getAlwaysAcceptor() {
        return new UserAcceptor() {
            public boolean userAccepts(FileDescription.FileContainer file) {
                return true;
            }
        };
    }

    public FileContentHandler(File file, boolean sending, UserAcceptor userAcceptor, File saveDir) {
        this.file = file;
        this.sending = sending;
        this.userAcceptor = userAcceptor;
        this.saveDir = saveDir;
    }
    
    public void setSaveDir(File saveDir) {
        this.saveDir = saveDir;
    }
    
    public void setProgressListener(FileTransferProgressListener progressListener) {
        this.progressListener = progressListener;
    }
    
    
    public JingleMediaSession createMediaSession(JingleSession jingleSession) {
        FileMediaNegotiator.JingleFile file = ((FileMediaNegotiator)mediaNegotiator).getFile();
        boolean sending = ((FileMediaNegotiator)mediaNegotiator).isSending();
        TransportCandidate bestRemoteCandidate = transportNegotiator.getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = transportNegotiator.getAcceptedLocalCandidate();        
        return new FileMediaSession(file, sending, bestRemoteCandidate, acceptedLocalCandidate, jingleSession, saveDir, progressListener);
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return new ICETransportManager(connection, "jstun.javawi.de", 3478);
    }

    protected MediaNegotiator createMediaNegotiator(JingleSession session) {
        return new FileMediaNegotiator(session, file, sending, userAcceptor);
    }
}
