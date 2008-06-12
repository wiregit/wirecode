/**
 * $RCSfile: ScreenShareSession.java,v $
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
package org.jivesoftware.smackx.jingle.sshare;

import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.sshare.api.ImageDecoder;
import org.jivesoftware.smackx.jingle.sshare.api.ImageEncoder;
import org.jivesoftware.smackx.jingle.sshare.api.ImageReceiver;
import org.jivesoftware.smackx.jingle.sshare.api.ImageTransmitter;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.DatagramSocket;

/**
 * This Class implements a complete JingleMediaSession.
 * It sould be used to transmit and receive captured images from the Display.
 * This Class should be automaticly controlled by JingleSession.
 * For better NAT Traversal support this implementation don't support only receive or only transmit.
 * To receive you MUST transmit. So the only implemented and functionally methods are startTransmit() and stopTransmit()
 *
 * @author Thiago Camargo
 */
public class ScreenShareSession extends JingleMediaSession {

    private ImageTransmitter transmitter = null;
    private ImageReceiver receiver = null;
    private int width = 600;
    private int height = 600;

    /**
     * Creates a org.jivesoftware.jingleaudio.jmf.AudioMediaSession with defined payload type, remote and local candidates
     *
     * @param payloadType Payload of the jmf
     * @param remote      the remote information. The candidate that the jmf will be sent to.
     * @param local       the local information. The candidate that will receive the jmf
     * @param locator     media locator
     */
    public ScreenShareSession(final TransportCandidate remote,
            final TransportCandidate local,JingleSession jingleSession) {
        super(remote, local, jingleSession);
        initialize();
    }

    protected void initialize(String ip, String localIp, int localPort, int remotePort) {
        if (this.getJingleSession() instanceof IncomingJingleSession) {

            JFrame window = new JFrame();
            JPanel jp = new JPanel();
            window.add(jp);

            window.setLocation(0, 0);
            window.setSize(600, 600);

            window.addWindowListener(new WindowAdapter(){
                public void windowClosed(WindowEvent e) {
                    receiver.stop();
                }
            });

            try {
                receiver = new ImageReceiver(InetAddress.getByName("0.0.0.0"), remotePort, localPort, width, height);
                System.out.println("Receiving on:" + localPort);
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }

            jp.add(receiver);
            receiver.setVisible(true);
            window.setAlwaysOnTop(true);
            window.setVisible(true);
        }
        else {
            try {
                InetAddress remote = InetAddress.getByName(ip);
                transmitter = new ImageTransmitter(new DatagramSocket(localPort), remote, remotePort, new Rectangle(0, 0, width, height));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        new Thread(transmitter).start();
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active active state
     */
    public void setTrasmit(boolean active) {
        transmitter.setTransmit(true);
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
        if(transmitter!=null){
            transmitter.stop();
        }
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void stopReceive() {
        if(receiver!=null){
            receiver.stop();
        }
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

    public void setEncoder(ImageEncoder encoder) {
        if (encoder != null) {
            this.transmitter.setEncoder(encoder);
        }
    }

    public void setDecoder(ImageDecoder decoder) {
        if (decoder != null) {
            this.receiver.setDecoder(decoder);
        }
    }
}
