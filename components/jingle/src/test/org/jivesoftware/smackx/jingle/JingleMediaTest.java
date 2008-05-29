package org.jivesoftware.smackx.jingle;

/**
 * $RCSfile: JingleMediaTest.java,v $
 * $Revision: 1.1.2.2 $
 * $Date: 09/11/2006
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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import javax.media.MediaLocator;
import javax.media.format.AudioFormat;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.audiortp.AudioChannel;
import org.jivesoftware.smackx.jingle.audiortp.AudioRTPContentHandler;
import org.jivesoftware.smackx.jingle.audiortp.PayloadType;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionStateListener;
import org.jivesoftware.smackx.jingle.nat.BridgedTransportManager;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.MockAudioRTPContentHandler;
import org.jivesoftware.smackx.jingle.nat.MockAudioRTPDescription;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.JingleError;

/**
 * Test the Jingle Media using the high level API
 * </p>
 *
 * @author Thiago Camargo
 */
public class JingleMediaTest extends SmackTestCase {

    public JingleMediaTest(final String name) {
        super(name);
    }

    public void testCompleteJmf() {

        XMPPConnection x0 = getConnection(0);
        XMPPConnection x1 = getConnection(1);

        for (int i = 0; i < 1; i++)
            try {

                ICETransportManager icetm0 = new ICETransportManager(x0, "jivesoftware.com", 3478);
                ICETransportManager icetm1 = new ICETransportManager(x1, "jivesoftware.com", 3478);

                final JingleManager jm0 = new JingleManager(x0);
                final JingleManager jm1 = new JingleManager(x1);

                jm0.addCreationListener(icetm0);
                jm1.addCreationListener(icetm1);

                JingleSessionRequestListener jingleSessionRequestListener = new JingleSessionRequestListener() {
                    public void sessionRequested(final JingleSessionRequest request) {
                        try {
                            IncomingJingleSession session = request.accept();
                            session.start(request);

                            session.addStateListener(new JingleSessionStateListener() {
                                public void beforeChange(JingleNegotiator.State old, JingleNegotiator.State newOne) throws JingleNegotiator.JingleException {
                                    if (newOne instanceof IncomingJingleSession.Active) {
                                        throw new JingleNegotiator.JingleException();
                                    }
                                }

                                public void afterChanged(JingleNegotiator.State old, JingleNegotiator.State newOne) {

                                }
                            });

                        }
                        catch (XMPPException e) {
                            e.printStackTrace();
                        }

                    }
                };

                jm1.addJingleSessionRequestListener(jingleSessionRequestListener);

                Content content = new Content();
                OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser(), null );

                js0.start();

                Thread.sleep(20000);

                IncomingJingleSession incomingJingleSession = (IncomingJingleSession) jm1.getSession(js0.getConnection().getUser());
                incomingJingleSession.removeAllStateListeners();

                Thread.sleep(15000);

                js0.terminate();

                jm1.removeJingleSessionRequestListener(jingleSessionRequestListener);

                Thread.sleep(60000);

            }
            catch (Exception e) {
                e.printStackTrace();
            }

    }

    public void testCompleteMulti() {

        try {

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            final JingleManager jm0 = new JingleManager(x0);
            final JingleManager jm1 = new JingleManager(x1);

            AudioRTPContentHandler handler = new AudioRTPContentHandler();
            List<PayloadType.Audio> payloads = handler.getSupportedPayloads();
            List<PayloadType.Audio> client1Payloads = new ArrayList<PayloadType.Audio>(payloads);
            List<PayloadType.Audio> client2Payloads = new ArrayList<PayloadType.Audio>(payloads);
            PayloadType.Audio payload = client1Payloads.remove(1);
            client1Payloads.add(0, payload);
            payload = client2Payloads.remove(2);
            client2Payloads.add(0, payload);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {
                        IncomingJingleSession session = request.accept();
                        try {
                            Thread.sleep(12000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            for (int i = 0; i < 10; i++) {

                OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser(), new AudioRTPContentHandler(client1Payloads.toArray(new PayloadType.Audio[]{})));

                js0.addStateListener(new JingleSessionStateListener() {

                    public void beforeChange(JingleNegotiator.State old, JingleNegotiator.State newOne) throws JingleNegotiator.JingleException {
                    }

                    public void afterChanged(JingleNegotiator.State old, JingleNegotiator.State newOne) {
                        if (newOne != null) {
                            if ((newOne instanceof OutgoingJingleSession.Active))
                                System.err.println("|||" + newOne.getClass().getCanonicalName() + "|||");
                        }
                    }
                });

                js0.start();

                Thread.sleep(45000);
                js0.terminate();

                Thread.sleep(1500);

            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteSpeex() {

        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            final JingleManager jm0 = new JingleManager(x0);
            final JingleManager jm1 = new JingleManager(x1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        IncomingJingleSession session = request.accept();

                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser(), new AudioRTPContentHandler(new PayloadType.Audio(15, "speex")));

            js0.start();

            Thread.sleep(150000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
      public void testCompleteScreenShare() {

        try {

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            final JingleManager jm0 = new JingleManager(x0);
            final JingleManager jm1 = new JingleManager(x1);

            ScreenShareContentHandler handler0 = new ScreenShareContentHandler();
            ScreenShareContentHandler handler1 = new ScreenShareContentHandler();

            //jm0.setMediaManager(mediaManager0);
            //jm1.setMediaManager(mediaManager1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {
                        
                        IncomingJingleSession session = request.accept();

                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            // TODO REPLACE WITH ScreenShare content 
            // Content content = new Content(new MockAudioRTPDescription(getTestPayloads1(), new STUNTransportManager()));
            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser(), content);

            js0.start();

            Thread.sleep(150000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }  */

    public void testCompleteWithBridge() {

        for (int i = 0; i < 1; i += 2) {
            final int n = i;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {

                        XMPPConnection x0 = getConnection(n);
                        XMPPConnection x1 = getConnection(n + 1);

                        BridgedTransportManager btm0 = new BridgedTransportManager(x0);
                        BridgedTransportManager btm1 = new BridgedTransportManager(x1);

                        final JingleManager jm0 = new JingleManager(x0);
                        final JingleManager jm1 = new JingleManager(x1);

                        jm0.addCreationListener(btm0);
                        jm1.addCreationListener(btm1);

                        jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                            public void sessionRequested(final JingleSessionRequest request) {

                                try {
                                    IncomingJingleSession session = request.accept();

                                    session.start(request);
                                }
                                catch (XMPPException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                        OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser(), new MockAudioRTPContentHandler(btm0));

                        js0.start();

                        Thread.sleep(20000);

                        js0.sendFormattedError(JingleError.UNSUPPORTED_TRANSPORTS);

                        Thread.sleep(20000);

                        js0.terminate();

                        Thread.sleep(3000);

                        x0.disconnect();
                        x1.disconnect();

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
        }

        try {
            Thread.sleep(250000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCompleteWithBridgeB() {
        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            BridgedTransportManager btm0 = new BridgedTransportManager(x0);
            BridgedTransportManager btm1 = new BridgedTransportManager(x1);

            final JingleManager jm0 = new JingleManager(x0);
            final JingleManager jm1 = new JingleManager(x1);

            jm0.addCreationListener(btm0);
            jm1.addCreationListener(btm1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        IncomingJingleSession session = request.accept();
                        
                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser(), new MockAudioRTPContentHandler(btm0));

            js0.start();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            js0 = jm0.createOutgoingJingleSession(x1.getUser(), new MockAudioRTPContentHandler(btm0));

            js0.start();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            x0.disconnect();
            x1.disconnect();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testAudioChannelOpenClose() {
        for (int i = 0; i < 5; i++) {
            try {
                AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP),null);
                AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP),null);

                audioChannel0.start();
                audioChannel1.start();

                try {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                audioChannel0.stop();
                audioChannel1.stop();

                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void testAudioChannelStartStop() {

        try {
            AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP),null);
            AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP),null);

            for (int i = 0; i < 5; i++) {

                audioChannel0.start();
                audioChannel1.start();

                try {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                audioChannel0.stop();
                audioChannel1.stop();

                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}