package org.jivesoftware.smackx.jingle.states;

import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.nat.TransportNegotiator;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.XMPPException;

public class StartState implements State {

    private final PendingState pendingState;
    private final MediaNegotiator mediaNegotiator;
    private final TransportNegotiator transportNegotiator;

    public StartState(PendingState pendingState, MediaNegotiator mediaNegotiator, TransportNegotiator transportNegotiator) {
        this.pendingState = pendingState;
        this.mediaNegotiator = mediaNegotiator;
        this.transportNegotiator = transportNegotiator;
    }
    
    public State dispatch(Jingle jin) throws XMPPException {
        Jingle jout;
        if(jin == null) {
            jout = sendSessionInitiate();
        } else if(Jingle.Action.SESSIONINITIATE.equals(jin.getAction())) {
            jout = handleSessionInitiate(jin);
        }
        return pendingState;
    }

    private Jingle handleSessionInitiate(Jingle jin) throws XMPPException {
        Jingle jout = new Jingle(Jingle.Action.CONTENTACCEPT, new Content(Content.Creator.initiator, "foo", null)); // TODO
        jout.setType(IQ.Type.SET);
        mediaNegotiator.addDescriptionToContentAccept(jin, jout);
        transportNegotiator.addTransportToContentAccept(jin, jout);  // TODO pick TransportNegotiator based on incoming <transport>
        return jout;
    }

    private Jingle sendSessionInitiate() throws XMPPException {
        Jingle jout = new Jingle(Jingle.Action.SESSIONINITIATE, new Content(Content.Creator.initiator, "foo", null)); // TODO
        jout.setType(IQ.Type.SET);
        mediaNegotiator.addDescriptionToSessionInitiate(jout);
        transportNegotiator.addTransportToSessionInitiate(jout);
        return jout;
    }
}
