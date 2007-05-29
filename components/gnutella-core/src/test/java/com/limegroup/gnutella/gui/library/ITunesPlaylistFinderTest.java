package com.limegroup.gnutella.gui.library;

import java.io.File;
import java.util.List;

import com.limegroup.gnutella.gui.mp3.PlayList;

import junit.framework.TestCase;

public class ITunesPlaylistFinderTest extends TestCase {

    public void testJeff() {
        File xml = new File("gui/iTunesTest.xml");
        List<PlayList> pls = new ITunesPlaylistFinder().findPlayLists(xml);
        for (PlayList pl : pls) {
            System.out.println(pl.getName());
            for (File f : pl.getSongs()) {
                System.out.println(" - " + f.getAbsolutePath());
            }
        }
    }
}
