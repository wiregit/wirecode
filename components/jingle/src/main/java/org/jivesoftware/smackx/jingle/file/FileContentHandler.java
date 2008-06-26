package org.jivesoftware.smackx.jingle.file;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.file.FileDescription;


public abstract class FileContentHandler extends JingleContentHandler {
    
    private FileDescription.FileContainer file;
    private UserAcceptor userAcceptor;
    private FileLocator fileLocator;
    private FileTransferProgressListener progressListener;


    protected FileContentHandler(FileDescription.FileContainer file, UserAcceptor userAcceptor, FileLocator fileLocator) {
        this.file = file;
        this.userAcceptor = userAcceptor;
        this.fileLocator = fileLocator;
    }

    protected static UserAcceptor getAlwaysAcceptor() {
        return new UserAcceptor() {
            public boolean userAccepts(FileDescription.FileContainer file) {
                return true;
            }
        };
    }
    
    protected abstract boolean isInitiator();
    
    public void setFileLocator(FileLocator fileLocator) {
        this.fileLocator = fileLocator;
    }
    
    public void setProgressListener(FileTransferProgressListener progressListener) {
        this.progressListener = progressListener;
    }
    
    
    public JingleMediaSession createMediaSession(JingleSession jingleSession) {
        FileDescription.FileContainer file = ((FileMediaNegotiator)mediaNegotiator).getFile();
        TransportCandidate bestRemoteCandidate = transportNegotiator.getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = transportNegotiator.getAcceptedLocalCandidate();        
        return new FileMediaSession(file, isInitiator(), bestRemoteCandidate, acceptedLocalCandidate, jingleSession, fileLocator, progressListener);
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return new ICETransportManager(connection, "jstun.javawi.de", 3478);
    }

    protected MediaNegotiator createMediaNegotiator(JingleSession session) {
        return new FileMediaNegotiator(session, file, userAcceptor);
    }
}
