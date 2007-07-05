package com.limegroup.gnutella.gui.mp3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

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
    
    public void testLoadFileException(){
        
        try {
            player.loadSong( new File(""));
        } catch (IOException e) {
            return;
        } catch (UnsupportedAudioFileException e) {
            return;
        } catch (LineUnavailableException e) {
            return;
        }
        fail("Excepted IOException");
    }
    
    public void testLoadStreamException(){
        
        InputStream stream = null;
        
        try {
            File f=  new File("com\\limegroup\\gnutella\\metadata\\corruptFileWithBadHeaders.mp3");
            stream = f.toURI().toURL().openStream();
        } catch (IOException e1) {
            fail("Failed to load song");
        }       

        try {
            player.loadSong( stream);
        } catch (IOException e) {
            return;
        } catch (UnsupportedAudioFileException e) {
            return;
        } catch (LineUnavailableException e) {
            return;
        }
        
        fail("Expected IOException");
    }
    
    public void testVolumeException(){

        try {
            player.setVolume(0);
        } catch (IOException e) {
            return;
        }
        fail("Expected IOException");
    }
    
    public void testPanException(){

        try {
            player.setBalance(0);
        } catch (IOException e) {
            return;
        }      
        fail("Expected IOException");
    }

}
