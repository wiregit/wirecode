package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smackx.jingle.audiortp.AudioRTPContentHandler;
import org.jivesoftware.smackx.jingle.audiortp.PayloadType;
import org.jivesoftware.smack.XMPPConnection;

import java.util.List;

public class MockAudioRTPContentHandler extends AudioRTPContentHandler {

    private final JingleTransportManager transportManager;

    public MockAudioRTPContentHandler(List<PayloadType.Audio> testPayloads, JingleTransportManager transportManager) {
        super();
        this.transportManager = transportManager;
        if(testPayloads != null) {
            payloads.clear();
            payloads.addAll(testPayloads);
        }
    }
    
    public MockAudioRTPContentHandler(JingleTransportManager transportManager) {
        this(null, transportManager);
    }

    protected JingleTransportManager createTransportManager(XMPPConnection connection) {
        return transportManager != null ? transportManager : super.createTransportManager(connection);
    }
}
