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

    public void setUp() {
        String dir = "com/limegroup/gnutella/mp3/";
        TEST_FILE = CommonUtils.getResourceFile(dir+"ID3TestFile.mp3");
    }

    public void tearDown() {
        TEST_FILE = null;
    }
    
    //////////

    /**
     * Tests that the ID3v2 tags are read correctly
     */
    public void testID3v2Tags() throws Exception {
        Object[] tags = new Object[7];
        Object[] params = {TEST_FILE, tags};
        Object ret = PrivilegedAccessor.invokeMethod(ID3Reader.class, 
                                                    "parseID3v2Data", params);
        assertFalse("There are only 5 values not 7", 
                                               ((Boolean)ret).booleanValue());
        assertEquals("Incorrect title", "Title 2", tags[0]);
        assertNull("Incorrect artist", tags[1]);
        assertNull("Incorrect album", tags[2]);
        assertEquals("Incorrect year", "2002", tags[3]);
        assertEquals("Incorrect track", new Short("12"), tags[4]);
        assertEquals("Incorrect comments", "Comment 2", tags[5]);
        assertEquals("Incorrect genre", "Acid", tags[6]);
    }

    /**
     * Tests that v2 tags are given presedence over v1 tags when they bost
     * exist, and that if a tag exists for v1 but not in v2, we use the v1
     * value
     */
    public void testOverAllReading() throws Exception {
        Object[] params = {TEST_FILE};
        Object retArray = 
        PrivilegedAccessor.invokeMethod(ID3Reader.class, "parseFile", params);
        Object[] ret = (Object[]) retArray;
        //Test if the values are as expected
        assertEquals("Incorrent size of array", 9, ret.length);
        assertEquals("Title not picked up correctly", "Title 2", ret[0]);
        assertEquals("Artist not picked up correctly", "", ret[1]);
        assertEquals("Album not picked up correctly", "Album 1", ret[2]);
        assertEquals("Year not picked up correctly", "2002", ret[3]);
        assertEquals("Track not picked up correctly", new Short("12"), ret[4]);
        assertEquals("Comment not picked up correctly", "Comment 2", ret[5]);
        assertEquals("Genre not picked up correctly", "Acid", ret[6]);
    }
    
}
