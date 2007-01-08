package com.limegroup.gnutella.metadata;


import java.io.File;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class ID3V2WritingTest extends LimeTestCase {

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
        String dir = "com/limegroup/gnutella/metadata/";
        File f = CommonUtils.getResourceFile(dir + "ID3EditorTestFile.mp3");
        assertTrue(f.exists());
        TEST_FILE = new File(TEST_NAME);
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
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
        AudioMetaData data = null;
        
        //1. Test that the values we read initially were correct.
        /*data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
               ID3Reader.class, "parseID3v2Data", new Object[] { TEST_FILE });*/
        data = (AudioMetaData)MetaData.parse(TEST_FILE);
        
        assertFalse(data.toString(), data.isComplete());
        assertEquals(data.toString(), "Title 2", data.getTitle());
        assertNull(data.toString(), data.getArtist());
        assertNull(data.toString(), data.getAlbum());
        assertEquals(data.toString(), "2002", data.getYear());
        assertEquals(data.toString(), 12, data.getTrack());
        assertEquals(data.toString(), "Comment 2", data.getComment());
        assertEquals(data.toString(), "Acid", data.getGenre());
        
        //2. Write new data into the file 
        MP3DataEditor editor = new MP3DataEditor();
        PrivilegedAccessor.setValue(editor, "title_", "New Title");
        PrivilegedAccessor.setValue(editor, "artist_", "new Artist");
        PrivilegedAccessor.setValue(editor, "genre_", "Classic Rock");
        
        editor.commitMetaData(TEST_FILE.getAbsolutePath());
        
        //3. Test if the data was written correctly
        data = (AudioMetaData)MetaData.parse(TEST_FILE);

        assertFalse(data.toString(), data.isComplete());
        assertEquals("Title not written", "New Title", data.getTitle());
        assertEquals("Artist not written", "new Artist",  data.getArtist());
        assertEquals("Incorrect genre ", "Classic Rock", data.getGenre());
        assertNull("Incorrect album", data.getAlbum());
        assertNull("Incorrect year", data.getYear());
        assertLessThan("Incorrect track", 0, data.getTrack());
        assertNull("Incorrect comment", data.getComment());

        MP3DataEditor editor2 = new MP3DataEditor();
        PrivilegedAccessor.setValue(editor2, "title_", "Title 2");
        PrivilegedAccessor.setValue(editor2, "year_", "2002");
        PrivilegedAccessor.setValue(editor2, "track_", "12");
        PrivilegedAccessor.setValue(editor2, "comment_", "Comment 2");
        PrivilegedAccessor.setValue(editor2, "genre_", "Acid");
        editor2.commitMetaData(TEST_FILE.getAbsolutePath());
    }
    
}
