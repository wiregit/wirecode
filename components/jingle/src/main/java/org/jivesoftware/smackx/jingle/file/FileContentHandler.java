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


public abstract class FileContentHandler extends JingleContentHandler {
    
    private FileDescription.FileContainer file;
    private UserAcceptor userAcceptor;
    private File saveDir;
    private FileTransferProgressListener progressListener;

    protected FileContentHandler(File file, boolean sending, File saveDir) {
        this(getFileContainer(file, sending), getAlwaysAcceptor(), saveDir);
    }

    private static FileDescription.FileContainer getFileContainer(File file, boolean sending) {
        return sending ? new FileDescription.Offer(new FileMediaNegotiator.JingleFile(file)) : new FileDescription.Request(new FileMediaNegotiator.JingleFile(file));
    }

    private static UserAcceptor getAlwaysAcceptor() {
        return new UserAcceptor() {
            public boolean userAccepts(FileDescription.FileContainer file) {
                return true;
            }
        };
    }

    protected FileContentHandler(FileDescription.FileContainer file, UserAcceptor userAcceptor, File saveDir) {
        this.file = file;
        this.userAcceptor = userAcceptor;
        this.saveDir = saveDir;
    }
    
    protected abstract boolean isInitiator();
    
    public void setSaveDir(File saveDir) {
        this.saveDir = saveDir;
    }
    
    public void setProgressListener(FileTransferProgressListener progressListener) {
        this.progressListener = progressListener;
    }
    
    
    public JingleMediaSession createMediaSession(JingleSession jingleSession) {
        FileDescription.FileContainer file = ((FileMediaNegotiator)mediaNegotiator).getFile();
        TransportCandidate bestRemoteCandidate = transportNegotiator.getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = transportNegotiator.getAcceptedLocalCandidate();        
        return new FileMediaSession(file, isInitiator(), bestRemoteCandidate, acceptedLocalCandidate, jingleSession, saveDir, progressListener);
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return new ICETransportManager(connection, "jstun.javawi.de", 3478);
    }

    protected MediaNegotiator createMediaNegotiator(JingleSession session) {
        return new FileMediaNegotiator(session, file, userAcceptor);
    }
}
