package com.limegroup.gnutella.gui.mp3;

import java.io.IOException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class AudioPlayerTest extends BaseTestCase {

    static AudioPlayer player;
    
    public AudioPlayerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AudioPlayerTest.class);
    }

    public static void globalSetUp() {
        player = new LimewirePlayer();
        
        assertNotNull(player);
    }
    
    public void testVolumeException(){

        try {
            player.setVolume(0);
        } catch (IOException e) {
            return;
        }
        fail("Expected IOException");
    }
}
