package org.jivesoftware.smackx.jingle.sshare;

import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.sshare.ScreenShareSession;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smack.XMPPConnection;

public class ScreenShareContentHandler extends JingleContentHandler {

    public MediaNegotiator getMediaNegotiator(JingleSession session) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public JingleMediaSession createMediaSession(JingleSession jingleSession) {
        TransportCandidate bestRemoteCandidate = transportNegotiator.getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = transportNegotiator.getAcceptedLocalCandidate();
        
        return new ScreenShareSession(bestRemoteCandidate, acceptedLocalCandidate, jingleSession);
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected MediaNegotiator createMediaNegotiator(JingleSession session) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
