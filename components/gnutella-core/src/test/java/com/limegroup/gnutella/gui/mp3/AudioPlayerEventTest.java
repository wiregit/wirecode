package com.limegroup.gnutella.gui.mp3;

import junit.framework.Test;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.gui.mp3.AudioPlayerEvent.PlayerState;

public class AudioPlayerEventTest extends BaseTestCase {

    public AudioPlayerEventTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AudioPlayerEventTest.class);
    }
    
    public void testUnknown() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.UNKNOWN, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.UNKNOWN);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testOpening() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.OPENING, -1);
        
        assertEquals(event.getDescription(), null);
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.OPENING);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), -1.0);
    }
    
    public void testOpened() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.OPENED, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.OPENED);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testPlaying() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.PLAYING, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.PLAYING);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testStopped() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.STOPPED, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.STOPPED);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testPaused() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.PAUSED, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.PAUSED);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testResumed() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.RESUMED, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.RESUMED);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testSeeking() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.SEEKING, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.SEEKING);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testEOM() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.EOM, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.EOM);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testPan() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.PAN, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.PAN);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testGain() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.GAIN, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.GAIN);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
    
    public void testSeeked() {
        AudioPlayerEvent event = new AudioPlayerEvent("object", 
                PlayerState.SEEKED, -1, 1.0, "description");
        
        assertEquals(event.getDescription(), "description");
        assertEquals(event.getSource(), "object");
        assertEquals(event.getState(), PlayerState.SEEKED);
        assertEquals(event.getPosition(), -1);
        assertEquals(event.getValue(), 1.0);
    }
}
