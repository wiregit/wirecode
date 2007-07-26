package com.limegroup.gnutella.gui.mp3;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class PlayListItemTest extends BaseTestCase {

    public PlayListItemTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PlayListItemTest.class);
    }
    
    public void testItem(){
        URL url = null;
        try {
            url = new URL("http:\\test.txt");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        PlayListItem item = new PlayListItem(url, "test", false, false);
        
        assertEquals(item.getURL(), url);
        assertEquals(item.getName(), "test");
        assertEquals(item.isFile(), false);
        assertEquals(item.isStorePreview(), false);
        assertEquals(item.getProperties(), null);
    }
    
    public void testConstructor(){
        URL url = null;
        try {
            url = new URL("http:\\test.txt");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String album = "album";
        String artist = "artist";
        String bitrate = "128";
        String comment = "comments";
        String copyright = "copyright holder";
        String genre = "genre";
        String length = "min:sec";
        String size = "200000";
        String title = "title";
        String track = "3";
        String type = "mp3";
        String year = "2000";
        
        
        Map<String,String> map = new HashMap<String,String>();
        map.put(PlayListItem.ALBUM, album);
        map.put(PlayListItem.ARTIST, artist);
        map.put(PlayListItem.BITRATE, bitrate);
        map.put(PlayListItem.COMMENT, comment);
        map.put(PlayListItem.COPYRIGHT, copyright);
        map.put(PlayListItem.GENRE, genre);
        map.put(PlayListItem.LENGTH, length);
        map.put(PlayListItem.SIZE, size);
        map.put(PlayListItem.TITLE, title);
        map.put(PlayListItem.TRACK, track);
        map.put(PlayListItem.TYPE, type);
        map.put(PlayListItem.YEAR, year);
        
        PlayListItem item = new PlayListItem(url, "test", true, true, map);
        
        assertEquals(item.getURL(), url);
        assertEquals(item.getName(), "test");
        assertEquals(item.isStorePreview(), true);
        assertEquals(item.isFile(), true);
        
        Map<String,String> props = item.getProperties();
        
        assertEquals(props.get(PlayListItem.ALBUM),album);
        assertEquals(props.get(PlayListItem.ARTIST),artist);
        assertEquals(props.get(PlayListItem.BITRATE),bitrate);
        assertEquals(props.get(PlayListItem.COMMENT),comment);
        assertEquals(props.get(PlayListItem.COPYRIGHT),copyright);
        assertEquals(props.get(PlayListItem.GENRE),genre);
        assertEquals(props.get(PlayListItem.LENGTH),length);
        assertEquals(props.get(PlayListItem.SIZE),size);
        assertEquals(props.get(PlayListItem.TITLE),title);
        assertEquals(props.get(PlayListItem.TRACK),track);
        assertEquals(props.get(PlayListItem.TYPE),type);
        assertEquals(props.get(PlayListItem.YEAR),year);
    }

}
