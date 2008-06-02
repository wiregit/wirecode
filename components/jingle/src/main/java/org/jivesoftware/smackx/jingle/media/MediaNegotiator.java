/**
 * $RCSfile: MediaNegotiator.java,v $
 * $Revision: 1.1.2.4 $
 * $Date: 2008-06-02 04:20:51 $
 *
 * Copyright 2003-2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle.media;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleNegotiator;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;
import org.jivesoftware.smackx.jingle.listeners.JingleMediaListener;
import org.jivesoftware.smackx.packet.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manager for jmf descriptor negotiation.
 * <p/>
 * <p/>
 * This class is responsible for managing the descriptor negotiation process,
 * handling all the xmpp packets interchange and the stage control.
 * handling all the xmpp packets interchange and the stage control.
 *
 * @author Thiago Camargo
 */
public abstract class MediaNegotiator extends JingleNegotiator {

    private static final Logger LOG = Logger.getLogger(MediaNegotiator.class);

    protected final JingleSession session;

    // states

    protected Inviting inviting;

    protected Accepting accepting;

    protected Pending pending;

    protected Active active;

    /**
     * Default constructor. The constructor establishes some basic parameters,
     * but it does not start the negotiation. For starting the negotiation, call
     * startNegotiation.
     *
     * @param js The jingle session.
     */
    public MediaNegotiator(JingleSession js) {
        super(js.getConnection());
        session = js;
        
    }
    
    public abstract void addDescriptionToSessionInitiate(Jingle jingle);
    
    public abstract void addDescriptionToContentAccept(Jingle jin, Jingle jout);

    /**
     * Dispatch an incomming packet. The medthod is responsible for recognizing
     * the packet type and, depending on the current state, deliverying the
     * packet to the right event handler and wait for a response.
     *
     * @param iq the packet received
     * @return the new Jingle packet to send.
     * @throws XMPPException
     */
    public IQ dispatchIncomingPacket(IQ iq, String id) throws XMPPException {
        IQ jout = null;

        if (nullState()) {
            if (iq == null) {
                // With a null packet, we are just inviting the other end...
                setState(inviting);
                jout = getState().eventInvite();
            }
            else {
                if (iq instanceof Jingle) {
                    // If there is no specific jmf action associated, then we
                    // are being invited to a new session...
                    setState(accepting);
                    jout = getState().eventInitiate((Jingle) iq);
                }
                else {
                    throw new IllegalStateException(
                            "Invitation IQ received is not a Jingle packet in Media negotiator.");
                }
            }
        }
        else {
            if (iq == null) {
                return null;
            }
            else {
                if (iq.getType().equals(IQ.Type.ERROR)) {
                    // Process errors
                    getState().eventError(iq);
                }
                else if (iq.getType().equals(IQ.Type.RESULT)) {
                    // Process ACKs
                    if (isExpectedId(iq.getPacketID())) {
                        jout = getState().eventAck(iq);
                        removeExpectedId(iq.getPacketID());
                    }
                }
                else if (iq instanceof Jingle) {
                    // Get the action from the Jingle packet
                    Jingle jin = (Jingle) iq;
                    Jingle.Action action = jin.getAction();

                    if (action != null) {
                        if (action.equals(Jingle.Action.CONTENTACCEPT)) {
                            jout = getState().eventAccept(jin);
                        } else if (action.equals(Jingle.Action.SESSIONACCEPT)) {
                            jout = getState().eventAccept(jin);
                        }
                        // TODO ??
                        /*else if (action.equals(Jingle.Action.SESSIONTERMINATE)) {
                            MediaNegotiator.triggerMediaClosed()
                        }*/
                        else if (action.equals(Jingle.Action.DESCRIPTIONINFO)) {
                            jout = getState().eventInfo(jin);
                        }
                        else if (action.equals(Jingle.Action.CONTENTMODIFY)) {
                            // TODO jout = getState().eventModify(jin);
                        }
                        // Any unknown action will be ignored: it is not a msg
                        // to us...
                    }
                }
            }
        }

        // Save the Id for any ACK
        if (id != null) {
            setExpectedId(id);
        }
        else {
            if (jout != null) {
                setExpectedId(jout.getPacketID());
            }
        }

        return jout;
    }

    /**
     * Return true if the content is negotiated.
     *
     * @return true if the content is negotiated.
     */
    public abstract boolean isEstablished();

    /**
     * Return true if the content is fully negotiated.
     *
     * @return true if the content is fully negotiated.
     */
    public boolean isFullyEstablished() {
        return isEstablished() && getState() == active;
    }

    /**
     * Trigger a session established event.
     *
     * @param bestPt payload type that has been agreed.
     */
    protected void triggerMediaEstablished(Description description) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleMediaListener) {
                JingleMediaListener mli = (JingleMediaListener) li;
                mli.mediaEstablished(description);
            }
        }
    }

    /**
     * Trigger a jmf closed event.
     *
     * @param currPt current payload type that is cancelled.
     */
    protected void triggerMediaClosed(Description description) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleMediaListener) {
                JingleMediaListener mli = (JingleMediaListener) li;
                mli.mediaClosed(description);
            }
        }
    }

    /**
     * Terminate the jmf negotiator
     */
    public void close() {
        super.close();
    }

    public void validate(Jingle jingle) throws XMPPException {
        List<Description> descriptions = jingle.getContent().getDescriptions();

        if (jingle.getAction().equals(Jingle.Action.SESSIONACCEPT)) {
            if (descriptions.size() != 1) {
                throw new XMPPException(
                        "Unsupported feature: the number of accepted content descriptions is greater than 1.");
            }
        }
    }

    public abstract void addAcceptedDescription(Content content);

    // States

    /**
     * First stage when we send a session request.
     */
    public abstract class Inviting extends JingleNegotiator.State {

        public Inviting(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * We have received the ACK for our invitation.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) {
            setState(pending);
            return null;
        }
    }

    /**
     * We are accepting connections.
     */
    public abstract class Accepting extends JingleNegotiator.State {

        public Accepting(MediaNegotiator neg) {
            super(neg);
        }

    }

    /**
     * Pending class: we are waiting for the other enpoint, that must say if it
     * accepts or not...
     */
    public abstract class Pending extends JingleNegotiator.State {

        public Pending(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * ACK received.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) {

            if (isEstablished()) {
                setState(active);
                return null;
            }
            return null;
        }
    }

    /**
     * "Active" state: we have an agreement about the codec...
     */
    public abstract class Active extends JingleNegotiator.State {

        public Active(MediaNegotiator neg) {
            super(neg);
        }

    }
}
