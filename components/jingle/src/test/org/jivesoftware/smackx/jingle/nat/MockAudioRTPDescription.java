package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.audiortp.PayloadType;
import org.jivesoftware.smackx.packet.audiortp.AudioRTPDescription;

import java.util.Arrays;
import java.util.List;

public class MockAudioRTPDescription extends AudioRTPDescription {
    
    List<PayloadType.Audio> testPayloads;
    private final JingleTransportManager transportManager;
    
    public MockAudioRTPDescription(List<PayloadType.Audio> testPayloads) {
        this(testPayloads, null);
    }

    public MockAudioRTPDescription(List<PayloadType.Audio> testPayloads, JingleTransportManager transportManager) {
        this.testPayloads = testPayloads;
        this.transportManager = transportManager;
    }
    
    public MockAudioRTPDescription(PayloadType.Audio testPayload, JingleTransportManager transportManager) {
        this(Arrays.asList(testPayload), transportManager);
    }
    
    public MockAudioRTPDescription(PayloadType.Audio testPayload) {
        this(testPayload, null);
    }
    
    public MockAudioRTPDescription(JingleTransportManager transportManager) {
        this.transportManager = transportManager;
    }

    public JingleContentHandler createContentHandler() {
        return new MockAudioRTPContentHandler(testPayloads, transportManager);
    }
}
