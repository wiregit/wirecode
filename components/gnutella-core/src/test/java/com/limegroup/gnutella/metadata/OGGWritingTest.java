package com.limegroup.gnutella.metadata;


import java.io.File;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
        String dir = "com/limegroup/gnutella/metadata/";
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
        
	assertEquals(0,retVal);

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
