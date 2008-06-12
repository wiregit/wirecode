package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportNegotiator;
import org.jivesoftware.smackx.jingle.nat.TransportResolver;

public abstract class JingleContentHandler {

    protected JingleTransportManager transportManager;
    protected TransportNegotiator transportNegotiator;
    protected MediaNegotiator mediaNegotiator;
    protected JingleMediaSession mediaSession;

    public JingleTransportManager getTransportManager(XMPPConnection connection) {
        if(transportManager == null) {
            transportManager = createTransportManager(connection);
        }
        return transportManager;
    }
    
    protected abstract JingleTransportManager createTransportManager(XMPPConnection connection);    
    
    public TransportNegotiator getTransportNegotiator(JingleSession session, XMPPConnection connection) throws XMPPException {
        if(transportNegotiator == null) {
            transportNegotiator = createTransportNegotiator(session);
        }
        return transportNegotiator;
    }
    
    protected TransportNegotiator createTransportNegotiator(JingleSession session) throws XMPPException {
        TransportResolver resolver = getTransportManager(session.getConnection()).getResolver(session);
        
        if (resolver.getType().equals(TransportResolver.Type.rawupd)) {
            transportNegotiator = new TransportNegotiator.RawUdp(session, resolver);
        } else if (resolver.getType().equals(TransportResolver.Type.ice)) {
            transportNegotiator = new TransportNegotiator.Ice(session, resolver);
        }
        // TODO IBB, SOCKS5, ICE-TCP
        return transportNegotiator;
    }
    
    public MediaNegotiator getMediaNegotiator(JingleSession session) {
        if(mediaNegotiator == null) {
            mediaNegotiator = createMediaNegotiator(session);
        }
        return mediaNegotiator;
    }
    
    protected abstract MediaNegotiator createMediaNegotiator(JingleSession session);
    
    public JingleMediaSession getMediaSession(JingleSession session) {
        if(mediaSession == null) {
            mediaSession = createMediaSession(session);
        }
        return mediaSession;
    }
    
    protected abstract JingleMediaSession createMediaSession(JingleSession jingleSession);

}
