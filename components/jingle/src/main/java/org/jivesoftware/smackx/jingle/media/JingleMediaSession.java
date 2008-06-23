/**
 * $RCSfile: JingleMediaSession.java,v $
 * $Revision: 1.1.4.2 $
 * $Date: 2008-06-23 20:31:57 $11-07-2006
 *
 * Copyright 2003-2006 Jive Software.
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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

/**
 * Public Abstract Class provides a clear interface between Media Session and Jingle API.
 * <p/>
 * When a Jingle Session is fully stablished, we will have a Payload Type and two transport candidates defined for it.
 * Smack Jingle API don't implement Media Transmit and Receive methods.
 * But provides an interface to let the user implements it using another API. For instance: JMF.
 * <p/>
 * <i>The Class that implements this one, must have the support to transmit and receive the jmf.</i>
 * <i>This interface let the user choose his own jmf API.</i>
 *
 * @author Thiago Camargo
 */
public abstract class JingleMediaSession {
    
    private static final Log LOG = LogFactory.getLog(JingleSession.class);
    
    // Local Transport details
    private TransportCandidate local;
    // Remote Transport details
    private TransportCandidate remote;
    // Media Received Listener
    private List<MediaReceivedListener> mediaReceivedListeners = new ArrayList<MediaReceivedListener>();
    // Jingle Session
    private JingleSession jingleSession;

    /**
     * Creates a new JingleMediaSession Instance to handle Media methods.
     *
     * @param payloadType  Payload Type of the transmittion
     * @param remote       Remote accepted Transport Candidate
     * @param local        Local accepted Transport Candidate
     * @param mediaLocator Media Locator of the capture device
     */
    public JingleMediaSession(TransportCandidate remote,
            TransportCandidate local, JingleSession jingleSession) {
        this.local = local;
        this.remote = remote;
        this.jingleSession = jingleSession;
    }

    /**
     * Returns the Media Session local Candidate
     *
     * @return
     */
    public TransportCandidate getLocal() {
        return local;
    }

    /**
     * Returns the Media Session remote Candidate
     *
     * @return
     */
    public TransportCandidate getRemote() {
        return remote;
    }

    /**
     * Adds a Media Received Listener
     *
     * @param mediaReceivedListener
     */
    public void addMediaReceivedListener(MediaReceivedListener mediaReceivedListener) {
        mediaReceivedListeners.add(mediaReceivedListener);
    }

    /**
     * Removes a Media Received Listener
     *
     * @param mediaReceivedListener
     */
    public void removeMediaReceivedListener(MediaReceivedListener mediaReceivedListener) {
        mediaReceivedListeners.remove(mediaReceivedListener);
    }

    /**
     * Removes all Media Received Listeners
     */
    public void removeAllMediaReceivedListener() {
        mediaReceivedListeners.clear();
    }
    
    protected abstract void initialize(String ip, String localIp, int localPort, int remotePort);

    /**
     * Initialize the Audio Channel to make it able to send and receive audio
     */
    public void initialize() {

        String ip;
        String localIp;
        int localPort;
        int remotePort;

        if (this.getLocal().getSymmetric() != null) {
            ip = local.getIp();
            localIp = local.getLocalIp();
            localPort = getFreePort();
            remotePort = local.getSymmetric().getPort();

            LOG.info(local.getConnection() + " " + ip + ": " + localPort + "->" + remotePort);

        }
        else {
            ip = remote.getIp();
            localIp = local.getLocalIp();
            localPort = local.getPort();
            remotePort = remote.getPort();
        }

        initialize(ip, localIp, localPort, remotePort);
    }

    /**
     * Starts a RTP / UDP / TCP Transmission to the remote Candidate
     */
    public abstract void startTrasmit();

    /**
     * Starts a RTP / UDP / TCP Receiver from the remote Candidate to local Candidate
     */
    public abstract void startReceive();

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active
     */
    public abstract void setTrasmit(boolean active);

    /**
     * Stops a RTP / UDP / TCP Transmission to the remote Candidate
     */
    public abstract void stopTrasmit();

    /**
     * Stops a RTP / UDP / TCP Receiver from the remote Candidate to local Candidate
     */
    public abstract void stopReceive();

    /**
     * Called when new Media is received.
     */
    public void mediaReceived(String participant) {
        for (MediaReceivedListener mediaReceivedListener : mediaReceivedListeners) {
            mediaReceivedListener.mediaReceived(participant);
        }
    }

    /**
     * Gets associated JingleSession
     * @return associated JingleSession
     */
    public JingleSession getJingleSession() {
        return jingleSession;
    }

    /**
     * Obtain a free port we can use.
     *
     * @return A free port number.
     */
    protected int getFreePort() {
        ServerSocket ss;
        int freePort = 0;

        for (int i = 0; i < 10; i++) {
            freePort = (int) (10000 + Math.round(Math.random() * 10000));
            freePort = freePort % 2 == 0 ? freePort : freePort + 1;
            try {
                ss = new ServerSocket(freePort);
                freePort = ss.getLocalPort();
                ss.close();
                return freePort;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ss = new ServerSocket(0);
            freePort = ss.getLocalPort();
            ss.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return freePort;
    }
}
