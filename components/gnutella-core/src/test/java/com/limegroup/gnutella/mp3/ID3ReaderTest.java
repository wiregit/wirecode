package com.limegroup.gnutella.mp3;


import junit.framework.Test;

import java.io.*;
import com.limegroup.gnutella.util.*;

public class ID3ReaderTest extends BaseTestCase {

    private static File TEST_FILE;

	public ID3ReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ID3ReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    ////////////
    public static void globalSetUp() {
        String dir = "com/limegroup/gnutella/mp3/";
        TEST_FILE = CommonUtils.getResourceFile(dir+"ID3TestFile.mp3");
        assertTrue("file should exist", TEST_FILE.exists());
    }
    
    //////////

    /**
     * Tests that the ID3v2 tags are read correctly
     */
    public void testID3v2Tags() throws Exception {
        ID3Reader.ID3Data data = null;
        
        data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
               ID3Reader.class, "parseID3v2Data", new Object[] { TEST_FILE });
               
        assertFalse(data.toString(), data.isComplete());
        assertEquals("Incorrect title", "Title 2", data.getTitle());
        assertEquals("Incorrect artist", null, data.getArtist());
        assertEquals("Incorrect album", null, data.getAlbum());
        assertEquals("Incorrect year", "2002", data.getYear());
        assertEquals("Incorrect track", 12, data.getTrack());
        assertEquals("Incorrect comments", "Comment 2", data.getComment());
        assertEquals("Incorrect genre", "Acid", data.getGenre());
    }

    /**
     * Tests that v2 tags are given presedence over v1 tags when they bost
     * exist, and that if a tag exists for v1 but not in v2, we use the v1
     * value
     */
    public void testOverAllReading() throws Exception {
        ID3Reader.ID3Data data = null;
        
        data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
               ID3Reader.class, "parseFile", new Object[] { TEST_FILE });

        //Test if the values are as expected
        assertFalse("Incorrent size of array", data.isComplete());
        assertEquals("bad title", "Title 2", data.getTitle());
        assertEquals("bad artist", "", data.getArtist());
        assertEquals("bad album", "Album 1", data.getAlbum());
        assertEquals("bad year", "2002", data.getYear());
        assertEquals("bad track", 12, data.getTrack());
        assertEquals("bad comment", "Comment 2", data.getComment());
        assertEquals("bad genre", "Acid", data.getGenre());
    }
    
}
