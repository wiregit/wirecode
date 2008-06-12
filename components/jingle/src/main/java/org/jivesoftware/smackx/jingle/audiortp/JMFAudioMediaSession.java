/**
 * $RCSfile: JMFAudioMediaSession.java,v $
 * $Revision: 1.1.4.1 $
 * $Date: 08/11/2006
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.jingle.audiortp;

import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.audiortp.PayloadType;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.JingleSession;

import javax.media.MediaLocator;

/**
 * This Class implements a complete JingleMediaSession.
 * It sould be used to transmit and receive audio captured from the Mic.
 * This Class should be automaticly controlled by JingleSession.
 * But you could also use in any VOIP application.
 * For better NAT Traversal support this implementation don't support only receive or only transmit.
 * To receive you MUST transmit. So the only implemented and functionally methods are startTransmit() and stopTransmit()
 *
 * @author Thiago Camargo
 */
public class JMFAudioMediaSession extends JingleMediaSession {
    
    // Payload Type of the Session
    private PayloadType.Audio payloadType;    
    // Media Locator
    private String mediaLocator;

    private AudioChannel audioChannel;

    /**
     * Creates a org.jivesoftware.jingleaudio.jmf.AudioMediaSession with defined payload type, remote and local candidates
     *
     * @param payloadType Payload of the jmf
     * @param remote      the remote information. The candidate that the jmf will be sent to.
     * @param local       the local information. The candidate that will receive the jmf
     * @param locator     media locator
     */
    public JMFAudioMediaSession(final PayloadType.Audio payloadType, final TransportCandidate remote,
            final TransportCandidate local, String locator, JingleSession jingleSession) {
        super(remote, local,jingleSession);
        this.payloadType = payloadType;
        this.mediaLocator = locator==null?"dsound://":locator;
        initialize();
    }

    protected void initialize(String ip, String localIp, int localPort, int remotePort) {
        audioChannel = new AudioChannel(new MediaLocator(mediaLocator), localIp, ip, localPort, remotePort, AudioFormatUtils.getAudioFormat(payloadType),this);
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        audioChannel.start();
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active active state
     */
    public void setTrasmit(boolean active) {
        audioChannel.setTrasmit(active);
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void startReceive() {
        // Do nothing
    }

    /**
     * Stops transmission and for NAT Traversal reasons stop receiving also.
     */
    public void stopTrasmit() {
        if (audioChannel != null)
            audioChannel.stop();
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void stopReceive() {
        // Do nothing
    }

    public PayloadType.Audio getPayloadType() {
        return payloadType;
    }
}
