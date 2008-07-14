package org.jivesoftware.smackx.jingle.audiortp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;
import org.jivesoftware.smackx.packet.audiortp.AudioRTPDescription;

public class AudioRTPMediaNegotiator extends MediaNegotiator {

//    private static final Logger LOG = Logger.getLogger(AudioRTPMediaNegotiator.class);

    private final List<PayloadType.Audio> localAudioPts = new ArrayList<PayloadType.Audio>();
    private final List<PayloadType.Audio> remoteAudioPts = new ArrayList<PayloadType.Audio>();
    private PayloadType.Audio bestCommonAudioPt;

    public AudioRTPMediaNegotiator(JingleSession js, List<PayloadType.Audio> pts) {
        super(js);
        bestCommonAudioPt = null;

        if (pts != null) {
            localAudioPts.addAll(pts);
        }
        inviting = new InvitingImpl(this);
        accepting = new AcceptingImpl(this);
        pending = new PendingImpl(this);
        active = new ActiveImpl(this);
    }

    public boolean isEstablished() {
        return getBestCommonAudioPt() != null;
    }
    
    private PayloadType.Audio calculateBestCommonAudioPt() {
        final ArrayList<PayloadType.Audio> commonAudioPts = new ArrayList<PayloadType.Audio>();       

        if (session.getInitiator().equals(session.getConnection().getUser())) {
            commonAudioPts.addAll(localAudioPts);
            commonAudioPts.retainAll(remoteAudioPts);            
        }
        else {
            commonAudioPts.addAll(remoteAudioPts);
            commonAudioPts.retainAll(localAudioPts);
        }
        if(!commonAudioPts.isEmpty()) {
            return commonAudioPts.get(0);
        } else {
            return null;
        }
    }

    /**
     * Adds a payload type to the list of remote payloads.
     *
     * @param pt the remote payload type
     */
    public void addRemoteAudioPayloadType(PayloadType.Audio pt) {
        if (pt != null) {
            synchronized (remoteAudioPts) {
                remoteAudioPts.add(pt);
            }
        }
    }

    public void addDescriptionToSessionInitiate(Jingle jingle) {
        AudioRTPDescription audioDescr = new AudioRTPDescription();
        audioDescr.addAudioPayloadTypes(localAudioPts);
        jingle.getContent().addDescription(audioDescr);
    }

    /**
     * Create an offer for the list of audio payload types.
     *
     * @return a new Jingle packet with the list of audio Payload Types
     */
    private Jingle getAudioPayloadTypesOffer() {
        AudioRTPDescription audioDescr = new AudioRTPDescription();

        // Add the list of payloads for audio and create a
        // AudioRTPDescription
        // where we announce our payloads...
        audioDescr.addAudioPayloadTypes(localAudioPts);

        Jingle jingle = new Jingle(new Content(audioDescr));
        jingle.setAction(Jingle.Action.DESCRIPTIONINFO);
        jingle.setType(IQ.Type.SET);
        return jingle;
    }

    /**
     * Create an IQ "accept" message.
     */
    private Jingle createAcceptMessage() {
        Jingle jout = null;

        // If we hava a common best codec, send an accept right now...
        jout = new Jingle(Jingle.Action.CONTENTACCEPT, new Content());
        jout.getContent().addDescription(new AudioRTPDescription(
                new AudioRTPDescription.JinglePayloadType(bestCommonAudioPt)));

        return jout;
    }

    /**
     * Get the best common codec between both parts.
     *
     * @return The best common PayloadType codec.
     */
    public PayloadType.Audio getBestCommonAudioPt() {
        return bestCommonAudioPt;
    }

    @SuppressWarnings("unchecked")
    private List obtainPayloads(Jingle jin) {
        List result = new ArrayList();
        Iterator iDescr = jin.getContent().getDescriptions().iterator();

        // Add the list of payloads: iterate over the descriptions...
        while (iDescr.hasNext()) {
            AudioRTPDescription descr = (AudioRTPDescription) iDescr
                    .next();

            if (descr != null) {
                // ...and, then, over the payloads.
                // Note: we use the last "description" in the packet...
                result.clear();
                result.addAll(descr.getAudioPayloadTypesList());
            }
        }

        return result;
    }

    public void validate(Jingle jingle) throws XMPPException {
        super.validate(jingle);
        List<Description> descriptions = jingle.getContent().getDescriptions();

        if (jingle.getAction().equals(Jingle.Action.SESSIONACCEPT)) {
            AudioRTPDescription jd = (AudioRTPDescription) descriptions.get(0);
            if (jd.getJinglePayloadTypesCount() > 1) {
                throw new XMPPException(
                        "Unsupported feature: the number of accepted payload types is greater than 1.");
            }
        }
    }

    public void addAcceptedDescription(Content content) {
        // TODO save Description and reuse for event listeners (instead of recreating)
        content.addDescription(new AudioRTPDescription(
                        new AudioRTPDescription.JinglePayloadType(bestCommonAudioPt)));
    }

    public class InvitingImpl extends Inviting {

        public InvitingImpl(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * Create an initial Jingle packet, with the list of payload types that
         * we support. The list is in order of preference.
         */
        public Jingle eventInvite() {
            return getAudioPayloadTypesOffer();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void addDescriptionToContentAccept(Jingle jin, Jingle jout) {
        synchronized (remoteAudioPts) {
            remoteAudioPts.addAll(obtainPayloads(jin));
        }
        AudioRTPDescription audioDescr = new AudioRTPDescription();
        audioDescr.addAudioPayloadTypes(localAudioPts);
        jout.getContent().addDescription(audioDescr);
    }
    
    public class AcceptingImpl extends Accepting {

        public AcceptingImpl(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * We have received an invitation! Respond with a list of our payload
         * types...
         */
        @SuppressWarnings("unchecked")
        public Jingle eventInitiate(Jingle jin) {
            synchronized (remoteAudioPts) {
                remoteAudioPts.addAll(obtainPayloads(jin));
            }

            return getAudioPayloadTypesOffer();
        }

        /**
         * Process the ACK of our list of codecs (our offer).
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) throws XMPPException {
            Jingle response = null;

            if (!remoteAudioPts.isEmpty()) {
                // Calculate the best common codec
                bestCommonAudioPt = calculateBestCommonAudioPt();

                // and send an accept if we havee an agreement...
                if (bestCommonAudioPt != null) {
                    response = createAcceptMessage();
                }
                else {
                    throw new JingleException(JingleError.NO_COMMON_PAYLOAD);
                }

                setState(pending);
            }

            return response;
        }
    }
    
    public class PendingImpl extends Pending {

        public PendingImpl(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * A content info has been received. This is done for publishing the
         * list of payload types...
         *
         * @param jin The input packet
         * @return a Jingle packet
         * @throws org.jivesoftware.smackx.jingle.JingleNegotiator.JingleException
         */
        @SuppressWarnings("unchecked")
        public Jingle eventInfo(Jingle jin) throws JingleException {
            PayloadType.Audio oldBestCommonAudioPt = bestCommonAudioPt;
            List offeredPayloads;
            Jingle response = null;
            boolean ptChange = false;

            offeredPayloads = obtainPayloads(jin);
            if (!offeredPayloads.isEmpty()) {

                synchronized (remoteAudioPts) {
                    remoteAudioPts.clear();
                    remoteAudioPts.addAll(offeredPayloads);
                }

                // Calculate the best common codec
                bestCommonAudioPt = calculateBestCommonAudioPt();
                if (bestCommonAudioPt != null) {
                    // and send an accept if we have an agreement...
                    ptChange = !bestCommonAudioPt.equals(oldBestCommonAudioPt);
                    if (oldBestCommonAudioPt == null || ptChange) {
                        response = createAcceptMessage();
                    }
                }
                else {
                    throw new JingleException(JingleError.NO_COMMON_PAYLOAD);
                }
            }

            // Parse the Jingle and get the payload accepted
            return response;
        }

        /**
         * A jmf description has been accepted. In this case, we must save the
         * accepted payload type and notify any listener...
         *
         * @param jin The input packet
         * @return a Jingle packet
         * @throws org.jivesoftware.smackx.jingle.JingleNegotiator.JingleException
         */
        public Jingle eventAccept(Jingle jin) throws JingleException {
            PayloadType.Audio agreedCommonAudioPt;
            List offeredPayloads = new ArrayList();
            Jingle response = null;

            if (bestCommonAudioPt == null) {
                // Update the best common audio PT
                bestCommonAudioPt = calculateBestCommonAudioPt();
                response = createAcceptMessage();
            }

            offeredPayloads = obtainPayloads(jin);
            if (!offeredPayloads.isEmpty()) {
                if (offeredPayloads.size() == 1) {
                    agreedCommonAudioPt = (PayloadType.Audio) offeredPayloads.get(0);
                    if (bestCommonAudioPt != null) {
                        // If the accepted PT matches the best payload
                        // everything is fine
                        if (!agreedCommonAudioPt.equals(bestCommonAudioPt)) {
                            throw new JingleException(JingleError.NEGOTIATION_ERROR);
                        }
                    }

                }
                else if (offeredPayloads.size() > 1) {
                    throw new JingleException(JingleError.MALFORMED_STANZA);
                }
            }

            return response;
        }

        /*
           * (non-Javadoc)
           *
           * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventError(org.jivesoftware.smack.packet.IQ)
           */
        public void eventError(IQ iq) throws XMPPException {
            triggerMediaClosed(new AudioRTPDescription(
                        new AudioRTPDescription.JinglePayloadType(bestCommonAudioPt)));
            super.eventError(iq);
        }
    }
    
    public class ActiveImpl extends Active {

        public ActiveImpl(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * We have an agreement.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventEnter()
         */
        public void eventEnter() {
            triggerMediaEstablished(new AudioRTPDescription(
                        new AudioRTPDescription.JinglePayloadType(bestCommonAudioPt)));
            System.err.println("BS:"+getBestCommonAudioPt().getName());
            super.eventEnter();
        }

        /**
         * We are breaking the contract...
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventExit()
         */
        public void eventExit() {
            triggerMediaClosed(new AudioRTPDescription(
                        new AudioRTPDescription.JinglePayloadType(bestCommonAudioPt)));
            super.eventExit();
        }
    }
}
