package com.limegroup.gnutella.mp3;


import junit.framework.Test;

import java.io.*;
import com.limegroup.gnutella.util.*;

public class ID3V2WritingTest extends BaseTestCase {

    private static File TEST_FILE;

	public ID3V2WritingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ID3V2WritingTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    ////////////

    public void setUp() {
        String dir = "com/limegroup/gnutella/mp3/";
        TEST_FILE = CommonUtils.getResourceFile(dir+"ID3EditorTestFile.mp3");
    }

//      public void tearDown() {
//          // we will check it out again next time the test runs
//          TEST_FILE = null;
//    }

    public static void globalTearDown() {
        boolean del = TEST_FILE.delete();
        if(!del) 
            TEST_FILE.deleteOnExit();
    }
    
    //////////

    /**
     * Tests that the ID3v2 tags are read correctly
     */
    public void testID3v2TagsWriting() throws Exception {
        //1. Test that the values we read initially were correct.
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
        
        //2. Write new data into the file 
        ID3Editor editor = new ID3Editor();
        PrivilegedAccessor.setValue(editor, "title_", "New Title");
        PrivilegedAccessor.setValue(editor, "artist_", "new Artist");
        PrivilegedAccessor.setValue(editor, "genre_", "Classic Rock");
        
        int retVal = editor.writeID3DataToDisk(TEST_FILE.getAbsolutePath());
        
        //3. Test if the data was written correctly
        Object[] tags2 = new Object[7];
        Object[] params2 = {TEST_FILE, tags2};
        ret = PrivilegedAccessor.invokeMethod(ID3Reader.class, 
                                                    "parseID3v2Data", params2);
        assertFalse("There are only 3 values not 7", 
                                               ((Boolean)ret).booleanValue());
        assertEquals("Title no written", "New Title", tags2[0]);
        assertEquals("Artist not written", "new Artist",  tags2[1]);
        assertEquals("Incorrect genre ", "Classic Rock", tags2[6]);        
        assertNull("Incorrect album", tags2[2]);
        assertNull("Incorrect year", tags2[3]);
        assertNull("Incorrect track", tags2[4]);
        assertNull("Incorrect comment", tags2[5]);

        ID3Editor editor2 = new ID3Editor();
        PrivilegedAccessor.setValue(editor2, "title_", "Title 2");
        PrivilegedAccessor.setValue(editor2, "year_", "2002");
        PrivilegedAccessor.setValue(editor2, "track_", "12");
        PrivilegedAccessor.setValue(editor2, "comment_", "Comment 2");
        PrivilegedAccessor.setValue(editor2, "genre_", "Acid");
        editor2.writeID3DataToDisk(TEST_FILE.getAbsolutePath());
    }
    
}
