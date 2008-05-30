/**
 * $RCSfile: IncomingJingleSession.java,v $
 * $Revision: 1.1.2.3 $
 * $Date: 2008-05-30 22:36:44 $
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.listeners.JingleMediaListener;
import org.jivesoftware.smackx.jingle.listeners.JingleTransportListener;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;
import org.jivesoftware.smackx.packet.Content;

/**
 * An incoming Jingle Session implementation.
 * This class has especific bahavior to accept and establish a received Jingle Session Request.
 * <p/>
 * This class is not directly used by users. Instead, users should refer to the
 * JingleManager class, that will create the appropiate instance...
 *
 * @author Alvaro Saurin
 * @author Thiago Camargo
 */
public class IncomingJingleSession extends JingleSession {

    // states
    private final Accepting accepting;

    private final Pending pending;

    private final Active active;

    private JingleSessionRequest initialSessionRequest;

    /**
     * Constructor for a Jingle incoming session
     *
     * @param conn                        the XMPP connection
     * @param responder                   the responder
     * @param transportManager            The transport manager
     * @param initialJingleSessionRequest the initial Jingle Session Request
     */
    protected IncomingJingleSession(XMPPConnection conn, String responder, JingleSessionRequest initialJingleSessionRequest) throws XMPPException {

        super(conn, responder, conn.getUser(), initialJingleSessionRequest.getSessionID(), initialJingleSessionRequest.getJingle().getContent().getDescriptions().get(0).getContentHandler());

        // Create the states...

        accepting = new Accepting(this);
        pending = new Pending(this);
        active = new Active(this);

        // Create description and transport negotiatiors...
        setMediaNeg(contentHandler.getMediaNegotiator(this));
        setTransportNeg(contentHandler.getTransportNegotiator(this, conn));

    }

    /**
     * Start the session for a initial Jingle request packet.
     *
     * @param initialJingleSessionRequest the initial Jingle Session Request
     * @throws XMPPException
     */
    public void start(JingleSessionRequest initialJingleSessionRequest) throws XMPPException {
        // Establish the default state
        setState(accepting);

        updatePacketListener();

        Jingle packet = initialJingleSessionRequest.getJingle();
        if (packet != null) {

            // Initialize the session information
            setSid(packet.getSid());

            respond(packet);
        }
        else {
            throw new XMPPException(
                    "Session request with null Jingle packet.");
        }

    }

    /**
     * Start the session using initial Jingle Session Request used to create this session..
     *
     * @throws XMPPException
     */
    public void start() throws XMPPException {
        start(getInitialSessionRequest());
    }
   
    /**
     * Get the initial Jingle packet request
     *
     * @return
     */
    public JingleSessionRequest getInitialSessionRequest() {
        return initialSessionRequest;
    }

    /**
     * Get the initial Jingle packet request
     *
     * @param initialRequest the initial Jingle packet
     */
    public void setInitialSessionRequest(JingleSessionRequest initialRequest) {
        this.initialSessionRequest = initialRequest;
    }

    // States

    /**
     * First stage when we have received a session request, and we accept the
     * request. We start in this stage, as the instance is created when the user
     * accepts the connection...
     */
    public class Accepting extends JingleNegotiator.State {

        public Accepting(JingleNegotiator neg) {
            super(neg);
        }

        /**
         * Initiate the incoming session. We have already sent the ACK partially
         * accepting the session...
         *
         * @throws XMPPException
         */
        public Jingle eventInitiate(Jingle inJingle) throws XMPPException {
            // Set the new session state
            setState(pending);
            return super.eventInitiate(inJingle);
        }

        /**
         * An error has occurred.
         *
         * @throws XMPPException
         */
        public void eventError(IQ iq) throws XMPPException {
            triggerSessionClosedOnError(new JingleException(iq.getError().getMessage()));
            super.eventError(iq);
        }

        /**
         * Terminate the connection.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventTerminate(org.jivesoftware.smackx.packet.Jingle)
         */
        public Jingle eventTerminate(Jingle jin) throws XMPPException {
            triggerSessionClosed("Closed Remotely");
            return super.eventTerminate(jin);
        }
    }

    /**
     * "Pending" state: we are waiting for the transport and content
     * negotiators.
     */
    public class Pending extends JingleNegotiator.State {

        JingleMediaListener jingleMediaListener;

        JingleTransportListener jingleTransportListener;

        public Pending(JingleNegotiator neg) {
            super(neg);

            // Create the listeners that will send a "session-accept" when the
            // sub-negotiators are done.
            jingleMediaListener = new JingleMediaListener() {
                public void mediaClosed(Description description) {
                }

                public void mediaEstablished(Description description) {
                    checkFullyEstablished();
                }
            };

            jingleTransportListener = new JingleTransportListener() {
                public void transportEstablished(TransportCandidate local,
                        TransportCandidate remote) {
                    checkFullyEstablished();
                }

                public void transportClosed(TransportCandidate cand) {
                }

                public void transportClosedOnError(XMPPException e) {
                }
            };
        }

        /**
         * Enter in the pending state: wait for the sub-negotiators.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventEnter()
         */
        public void eventEnter() {
            // Add the listeners to the sub-negotiators...
            addMediaListener(jingleMediaListener);
            addTransportListener(jingleTransportListener);
            super.eventEnter();
        }

        /**
         * Exit of the state
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventExit()
         */
        public void eventExit() {
            removeMediaListener(jingleMediaListener);
            removeTransportListener(jingleTransportListener);
            super.eventExit();
        }

        /**
         * Check if the session has been fully accepted by all the
         * sub-negotiators and, in that case, send an "accept" message...
         */
        private void checkFullyEstablished() {
            if (isFullyEstablished()) {

                TransportCandidate bestRemoteCandidate = getTransportNeg()
                        .getBestRemoteCandidate();

                TransportCandidate acceptedLocalCandidate = getTransportNeg()
                        .getAcceptedLocalCandidate();

                if (bestRemoteCandidate != null
                        && acceptedLocalCandidate != null) {
                    // Ok, send a packet saying that we accept this session
                    Jingle jout = new Jingle(Jingle.Action.SESSIONACCEPT);
                    jout.setContent(new Content());

                    // ... with the audio payload type and the transport
                    // candidate
                    getMediaNeg().addAcceptedDescription(jout.getContent());
                    jout.getContent().addTransport(getTransportNeg().getJingleTransport(
                            bestRemoteCandidate));

                    setExpectedId(jout.getPacketID());
                    sendFormattedJingle(jout);
                }
            }
        }

        /**
         * The other endpoint has accepted the session.
         */
        public Jingle eventAccept(Jingle jin) throws XMPPException {

            TransportCandidate acceptedLocalCandidate = null;

            // We process the "accepted" if we have finished the
            // sub-negotiators. Maybe this is not needed (ie, the other endpoint
            // can take the first valid transport candidate), but otherwise we
            // must cancel the negotiators...
            //
            if (isFullyEstablished()) {
                acceptedLocalCandidate = getAcceptedLocalCandidate(jin);

                if (acceptedLocalCandidate != null) {
                    if (acceptedLocalCandidate.equals(getTransportNeg()
                            .getAcceptedLocalCandidate())) {
                        getMediaNeg().validate(jin);
                        setState(active);
                    }
                }
                else {
                    throw new JingleException(JingleError.MALFORMED_STANZA);
                }
            }

            return super.eventAccept(jin);
        }

        /**
         * We have received a confirmation.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) throws XMPPException {
            setState(active);
            return super.eventAck(iq);
        }

        /**
         * An error has occurred.
         *
         * @throws XMPPException
         */
        public void eventError(IQ iq) throws XMPPException {
            if (iq == null) return;
            triggerSessionClosedOnError(new XMPPException(iq.getError()));
            super.eventError(iq);
        }

        /**
         * Terminate the connection.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventTerminate(org.jivesoftware.smackx.packet.Jingle)
         */
        public Jingle eventTerminate(Jingle jin) throws XMPPException {
            triggerSessionClosed("Closed Remotely");
            return super.eventTerminate(jin);
        }
    }

    /**
     * "Active" state: we have an agreement about the session.
     */
    public class Active extends JingleNegotiator.State {

        public Active(JingleNegotiator neg) {
            super(neg);
        }

        /**
         * We have a established session: notify the listeners
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventEnter()
         */
        public void eventEnter() {
            triggerSessionEstablished();

            super.eventEnter();
        }

        /**
         * Terminate the connection.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventTerminate(org.jivesoftware.smackx.packet.Jingle)
         */
        public Jingle eventTerminate(Jingle jin) throws XMPPException {
            triggerSessionClosed("Closed Remotely");
            return super.eventTerminate(jin);
        }

        /**
         * An error has occurred.
         *
         * @throws XMPPException
         */
        public void eventError(IQ iq) throws XMPPException {
            triggerSessionClosedOnError(new XMPPException(iq.getError().getMessage()));
            super.eventError(iq);
        }
    }

    public IQ sendFormattedError(JingleError error){
         IQ perror = null;
        if (error != null) {
            perror = createIQ(getSid(), getInitiator(), getResponder(), IQ.Type.ERROR);

            // Fill in the fields with the info from the Jingle packet
            perror.addExtension(error);

            getConnection().sendPacket(perror);
            System.err.println(perror.toXML());
        }
        return perror;
    }
}
