package com.limegroup.gnutella.metadata;


import junit.framework.Test;

import java.io.*;

import com.limegroup.gnutella.metadata.AudioMetaData;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaDataEditor;
import com.limegroup.gnutella.util.*;

public class OGGWritingTest extends BaseTestCase {

    private static String TEST_NAME = "testfile12341234.ogg";
    private static File TEST_FILE;

	public OGGWritingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(OGGWritingTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    ////////////

    public void setUp() {
        String dir = "com/limegroup/gnutella/mp3/";
        File f = CommonUtils.getResourceFile(dir + "oggAll.ogg");
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
    public void testOGGTagsWriting() throws Exception {
        AudioMetaData data = null;
        
        
        //2. Write data into the file 
        MetaDataEditor editor = MetaDataEditor.getEditorForFile(TEST_FILE.getPath());
        PrivilegedAccessor.setValue(editor, "title_", "New Title");
        PrivilegedAccessor.setValue(editor, "artist_", "new Artist");
        PrivilegedAccessor.setValue(editor, "genre_", "Classic Rock");
        
        int retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath());
        
        //3. Test if the data was written correctly
        data = (AudioMetaData)MetaData.parse(TEST_FILE);

        assertTrue(data.toString(), data.isComplete());
        assertEquals("Title not written", "New Title", data.getTitle());
        assertEquals("Artist not written", "new Artist",  data.getArtist());
        assertEquals("Incorrect genre ", "Classic Rock", data.getGenre());
        assertEquals("Incorrect album","allAlbum", data.getAlbum());
        assertEquals("Incorrect year", "1234",data.getYear());
        assertEquals("Incorrect track", 3, data.getTrack());
        assertEquals("Incorrect comment", "allComment",data.getComment());
    }
    
}
