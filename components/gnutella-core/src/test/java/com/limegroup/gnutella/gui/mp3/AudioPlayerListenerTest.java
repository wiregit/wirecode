package com.limegroup.gnutella.gui.mp3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class AudioPlayerListenerTest extends BaseTestCase {

    static LimewirePlayer player;
    static testAudioPlayerListener listener;
    
    public AudioPlayerListenerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AudioPlayerListenerTest.class);
    }
    
    public void setUp() {
        player = new LimewirePlayer();
        listener = new testAudioPlayerListener();
        player.addAudioPlayerListener(listener);
    }
    
    public void testStateUpdated(){
        player.fireStateUpdated( new AudioPlayerEvent(null, PlayerState.OPENED, 0) );
    
        assertEquals(1, listener.recievedEvents.size());

        player.removeAudioPlayerListener(listener);
        
        player.fireStateUpdated(new AudioPlayerEvent(null, PlayerState.OPENED, 0) );
        
        assertEquals(1, listener.recievedEvents.size());
    }
    
    public void testOpened(){
        player.fireOpened(null, null);
        
        assertEquals(1, listener.recievedEvents.size());
        
        player.removeAudioPlayerListener(listener);
        
        player.fireOpened(null, null);
        
        assertEquals(1, listener.recievedEvents.size());
    }
    
    public void testProgress(){
        player.fireProgress(-1, -1);
        
        assertEquals(1, listener.recievedEvents.size());
        
        player.removeAudioPlayerListener(listener);
        
        player.fireProgress(-1, -1);
        
        assertEquals(1, listener.recievedEvents.size());
    }
    
    public class testAudioPlayerListener implements AudioPlayerListener{

        List<Object> recievedEvents = new ArrayList<Object>();
        
        public void opened(AudioSource audioSource, Map<String, Object> properties) {
            recievedEvents.add(new opened(audioSource,properties));
        }

        public void progress(int bytesread, long microseconds) {
            recievedEvents.add(new progress(bytesread,microseconds));
        }

        public void stateChange(AudioPlayerEvent event) {
            recievedEvents.add(event);
        }
    }
    
    private class progress{
        int b;
        long s;
        AudioBuffer buff;
        Map<String,Object>m;
        
        public progress(int bytes, long sec){
            b = bytes;
            s = sec;
        }
    }
    
    private class opened{
        AudioSource s;
        Map<String,Object> m;
        
        public opened(AudioSource source, Map<String,Object> props){
            s = source;
            m = props;
        }
    }
}
