package com.limegroup.gnutella.mp3;


import junit.framework.Test;

import java.io.*;
import com.limegroup.gnutella.util.*;

public class ID3V2WritingTest extends BaseTestCase {

    private static String TEST_NAME = "testfile12341234.mp3";
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
        File f = CommonUtils.getResourceFile(dir + "ID3EditorTestFile.mp3");
        assertTrue(f.exists());
        TEST_FILE = new File(TEST_NAME);
        TEST_FILE.delete();
        CommonUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
    }

    public static void globalTearDown() {
        TEST_FILE.delete();
    }
    
    //////////

    /**
     * Tests that the ID3v2 tags are read correctly
     */
    public void testID3v2TagsWriting() throws Exception {
        ID3Reader.ID3Data data = null;
        
        //1. Test that the values we read initially were correct.
        data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
               ID3Reader.class, "parseID3v2Data", new Object[] { TEST_FILE });
        
        assertFalse(data.toString(), data.isComplete());
        assertEquals(data.toString(), "Title 2", data.getTitle());
        assertNull(data.toString(), data.getArtist());
        assertNull(data.toString(), data.getAlbum());
        assertEquals(data.toString(), "2002", data.getYear());
        assertEquals(data.toString(), 12, data.getTrack());
        assertEquals(data.toString(), "Comment 2", data.getComment());
        assertEquals(data.toString(), "Acid", data.getGenre());
        
        //2. Write new data into the file 
        ID3Editor editor = new ID3Editor();
        PrivilegedAccessor.setValue(editor, "title_", "New Title");
        PrivilegedAccessor.setValue(editor, "artist_", "new Artist");
        PrivilegedAccessor.setValue(editor, "genre_", "Classic Rock");
        
        int retVal = editor.writeID3DataToDisk(TEST_FILE.getAbsolutePath());
        
        //3. Test if the data was written correctly
        data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
               ID3Reader.class, "parseID3v2Data", new Object[] { TEST_FILE });

        assertFalse(data.toString(), data.isComplete());
        assertEquals("Title not written", "New Title", data.getTitle());
        assertEquals("Artist not written", "new Artist",  data.getArtist());
        assertEquals("Incorrect genre ", "Classic Rock", data.getGenre());
        assertNull("Incorrect album", data.getAlbum());
        assertNull("Incorrect year", data.getYear());
        assertLessThan("Incorrect track", 0, data.getTrack());
        assertNull("Incorrect comment", data.getComment());

        ID3Editor editor2 = new ID3Editor();
        PrivilegedAccessor.setValue(editor2, "title_", "Title 2");
        PrivilegedAccessor.setValue(editor2, "year_", "2002");
        PrivilegedAccessor.setValue(editor2, "track_", "12");
        PrivilegedAccessor.setValue(editor2, "comment_", "Comment 2");
        PrivilegedAccessor.setValue(editor2, "genre_", "Acid");
        editor2.writeID3DataToDisk(TEST_FILE.getAbsolutePath());
    }
    
}
