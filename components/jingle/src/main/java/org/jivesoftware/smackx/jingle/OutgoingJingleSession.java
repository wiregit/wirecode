/**
 * $RCSfile: OutgoingJingleSession.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-07-01 20:44:39 $
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

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.listeners.JingleMediaListener;
import org.jivesoftware.smackx.jingle.listeners.JingleTransportListener;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;

/**
 * An outgoing Jingle session implementation.
 * This class has especific bahavior to Request and establish a new Jingle Session.
 * <p/>
 * This class is not directly used by users. Instead, users should refer to the
 * JingleManager class, that will create the appropiate instance...
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 * @author Thiago Camargo
 */
public class OutgoingJingleSession extends JingleSession {

    private static final Logger LOG = Logger.getLogger(OutgoingJingleSession.class);

    // states

    private final Inviting inviting;

    private final Pending pending;

    private final Active active;

    /**
     * Constructor for a Jingle outgoing session.
     *
     * @param conn             the XMPP connection
     * @param responder        the other endpoint
     * @param payloadTypes     A list of payload types, in order of preference.
     * @param transportManager The transport manager.
     */
    protected OutgoingJingleSession(XMPPConnection conn, String responder,
            JingleContentHandler contentHandler) throws XMPPException {

        super(conn, conn.getUser(), responder, contentHandler);

        setSid(generateSessionId());

        // Initialize the states.
        inviting = new Inviting(this);
        pending = new Pending(this);
        active = new Active(this);               

        // Create description and transport negotiatiors...
        setMediaNeg(contentHandler.getMediaNegotiator(this));
        setTransportNeg(contentHandler.getTransportNegotiator(this, conn));
    }

    /**
     * Initiate the negotiation with an invitation. This method must be invoked
     * for starting all negotiations. It is the initial starting point and,
     * afterwards, any other packet processing is done with the packet listener
     * callback...
     *
     * @throws IllegalStateException
     */
    public void start(JingleSessionRequest req) throws IllegalStateException {
        if (nullState()) {
            setState(inviting);

            // Use the standard behavior, using a null Jingle packet
            try {
                updatePacketListener();
                respond((Jingle) null);
            }
            catch (XMPPException e) {
                e.printStackTrace();
                close();
            }
        }
        else {
            throw new IllegalStateException("Starting session without null state.");
        }
    }

    /**
     * Initiate the negotiation with an invitation. This method must be invoked
     * for starting all negotiations. It is the initial starting point and,
     * afterwards, any other packet processing is done with the packet listener
     * callback...
     *
     * @throws IllegalStateException
     */
    public void start() throws IllegalStateException {
        start(null);
    }

    // States

    /**
     * Current state when we want to invite the other endpoint.
     */
    public class Inviting extends JingleNegotiator.State {

        public Inviting(JingleNegotiator neg) {
            super(neg);
        }

        /**
         * Create an invitation packet.
         */
        public Jingle eventInvite() {
            // Create an invitation packet, saving the Packet ID, for any ACK
            return new Jingle(Jingle.Action.SESSIONINITIATE, new Content(Content.Creator.initiator, "foo", null)); // TODO
        }

        /**
         * The receiver has partially accepted our invitation. We go to the
         * pending state while the content and transport negotiators work...
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) {
            setState(pending);
            return null;
        }

        /**
         * The other endpoint has declined the invitation with an error.
         *
         * @throws XMPPException
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventError(org.jivesoftware.smack.packet.IQ)
         */
        public void eventError(IQ iq) throws XMPPException {
            triggerSessionDeclined(null);
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
     * <p/>
     * Note: the transition from/to this state is done with listeners...
     */
    public class Pending extends JingleNegotiator.State {

        JingleMediaListener jingleMediaListener;

        JingleTransportListener jingleTransportListener;

        public Pending(JingleNegotiator neg) {
            super(neg);

            // Create the listeners that will send a "session-accept" when
            // the sub-negotiators are done.
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
         * Enter in the pending state: install the listeners.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventEnter()
         */
        public void eventEnter() {
            // Add the listeners to the sub-negotiators...
            addMediaListener(jingleMediaListener);
            addTransportListener(jingleTransportListener);
        }

        /**
         * Exit of the state: remove the listeners.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventExit()
         */
        public void eventExit() {
            removeMediaListener(jingleMediaListener);
            removeTransportListener(jingleTransportListener);
        }

        /**
         * Check if the session has been fully accepted by all the
         * sub-negotiators and, in that case, send an "accept" message...
         */
        private void checkFullyEstablished() {

            if (isFullyEstablished()) {

                TransportCandidate bestRemoteCandidate = getTransportNeg()
                        .getBestRemoteCandidate();

                // Ok, send a packet saying that we accept this session
                // with the audio payload type and the transport
                // candidate
                Jingle jout = new Jingle(Jingle.Action.SESSIONACCEPT);
                jout.setContent(new Content());
                getMediaNeg().addAcceptedDescription(jout.getContent());
                jout.getContent().addTransport(getTransportNeg().getJingleTransport(
                        bestRemoteCandidate));

                // Send the "accept" and wait for the ACK
                setExpectedId(jout.getPacketID());
                sendFormattedJingle(jout);
            }
        }

        /**
         * The other endpoint has finally accepted our invitation.
         *
         * @throws XMPPException
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
                    throw new JingleException(JingleError.NEGOTIATION_ERROR);
                }
            }

            return null;
        }

        /**
         * We have received the Ack of our "accept"
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) {
            setState(active);
            return null;
        }

        /**
         * The other endpoint has rejected our invitation.
         *
         * @throws XMPPException
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

    /**
     * State when we have an established session.
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

    public IQ sendFormattedError(JingleError error) {
        IQ perror = null;
        if (error != null) {
            perror = createIQ(getSid(), getResponder(), getInitiator(), IQ.Type.ERROR);

            // Fill in the fields with the info from the Jingle packet
            perror.addExtension(error);

            getConnection().sendPacket(perror);
            System.err.println(perror.toXML());
        }
        return perror;
    }
}
