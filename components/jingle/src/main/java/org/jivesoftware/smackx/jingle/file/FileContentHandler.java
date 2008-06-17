package org.jivesoftware.smackx.jingle.file;

import java.io.File;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.file.FileMediaNegotiator;
import org.jivesoftware.smackx.jingle.file.FileMediaSession;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;


public class FileContentHandler extends JingleContentHandler {
    
    public enum ExchangeType { Request, Offer }
    
    private File file;
    private boolean sending;
    private UserAcceptor userAcceptor;
    
    public FileContentHandler(File file, boolean sending) {
        this(file, sending, getAllwaysAcceptor());
    }

    private static UserAcceptor getAllwaysAcceptor() {
        return new UserAcceptor() {
            public boolean userAccepts(FileDescription.FileContainer file) {
                return true;
            }
        };
    }

    public FileContentHandler(File file, boolean sending, UserAcceptor userAcceptor) {
        this.file = file;
        this.sending = sending;
        this.userAcceptor = userAcceptor;
    }
    
    
    public JingleMediaSession createMediaSession(JingleSession jingleSession) {
        StreamInitiation.File file = ((FileMediaNegotiator)mediaNegotiator).getFile();
        boolean sending = ((FileMediaNegotiator)mediaNegotiator).isSending();
        TransportCandidate bestRemoteCandidate = transportNegotiator.getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = transportNegotiator.getAcceptedLocalCandidate();        
        return new FileMediaSession(file, sending, bestRemoteCandidate, acceptedLocalCandidate, jingleSession);
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return new ICETransportManager(connection, "stun.xten.net", 3478);
    }

    protected MediaNegotiator createMediaNegotiator(JingleSession session) {
        return new FileMediaNegotiator(session, file, sending, userAcceptor);
    }
}
