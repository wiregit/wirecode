package org.jivesoftware.smackx.jingle.audiortp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

public class AudioRTPContentHandler extends JingleContentHandler {
    
    protected List<PayloadType.Audio> payloads = new ArrayList<PayloadType.Audio>();
    private String mediaLocator;
    
    public AudioRTPContentHandler() {
        this(new PayloadType.Audio(3, "gsm"), new PayloadType.Audio(4, "g723"), new PayloadType.Audio(0, "PCMU", 16000), new PayloadType.Audio(15, "speex"));
    }
    
    public AudioRTPContentHandler(PayloadType.Audio... payloads) {
        super();
        this.payloads.addAll(Arrays.asList(payloads));
    }
    
    public List<PayloadType.Audio> getSupportedPayloads() {
        return payloads;
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return new ICETransportManager(connection, "jstun.javawi.de", 3478);
    }

    protected MediaNegotiator createMediaNegotiator(JingleSession session) {
        return new AudioRTPMediaNegotiator(session, getSupportedPayloads());
    }

    public JingleMediaSession createMediaSession(final JingleSession jingleSession) {        
        PayloadType.Audio bestCommonAudioPt = ((AudioRTPMediaNegotiator)mediaNegotiator).getBestCommonAudioPt();
        TransportCandidate bestRemoteCandidate = transportNegotiator.getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = transportNegotiator.getAcceptedLocalCandidate();
        
        
        if(bestCommonAudioPt != null && bestCommonAudioPt.getName().equals("speex")) {
            return new JSpeexAudioMediaSession(bestRemoteCandidate, acceptedLocalCandidate, jingleSession);
        } else {
            return new JMFAudioMediaSession(bestCommonAudioPt, bestRemoteCandidate, acceptedLocalCandidate, mediaLocator, jingleSession);
        }
    }
    
    /**
     * Runs JMFInit the first time the application is started so that capture
     * devices are properly detected and initialized by JMF.
     */
    public static void setupJMF() {
        // .jmf is the place where we store the jmf.properties file used
        // by JMF. if the directory does not exist or it does not contain
        // a jmf.properties file. or if the jmf.properties file has 0 length
        // then this is the first time we're running and should continue to
        // with JMFInit
        String homeDir = System.getProperty("user.home");
        File jmfDir = new File(homeDir, ".jmf");
        String classpath = System.getProperty("java.class.path");
        classpath += System.getProperty("path.separator")
                + jmfDir.getAbsolutePath();
        System.setProperty("java.class.path", classpath);

        if (!jmfDir.exists())
            jmfDir.mkdir();

        File jmfProperties = new File(jmfDir, "jmf.properties");

        if (!jmfProperties.exists()) {
            try {
                jmfProperties.createNewFile();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // if we're running on linux checkout that libjmutil.so is where it
        // should be and put it there.
        runLinuxPreInstall();

        //if (jmfProperties.length() == 0) {
        new JMFInit(null, false);
        //}

    }

    private static void runLinuxPreInstall() {
        // @TODO Implement Linux Pre-Install
    }
}
