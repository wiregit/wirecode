package com.limegroup.gnutella.metadata;


import java.io.File;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class OGGWritingTest extends LimeTestCase {

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
    public void testOGGTagsWriting() throws Exception {
        AudioMetaData data = null;
        
        data = (AudioMetaData)MetaData.parse(TEST_FILE);
        assertEquals("Incorrect album","allAlbum", data.getAlbum());
        assertEquals("Incorrect year", "1234",data.getYear());
        assertEquals("Incorrect track", 3, data.getTrack());
        assertEquals("Incorrect comment", "allComment",data.getComment());        
        
        // Read existing stuff in file.
        LimeXMLDocument doc = ProviderHacks.getMetaDataReader().readDocument(TEST_FILE);
        //2. Write data into the file 
        MetaDataEditor editor = MetaDataEditor.getEditorForFile(TEST_FILE.getPath());
        editor.populate(doc);
        
        
        PrivilegedAccessor.setValue(editor, "title_", "New Title");
        PrivilegedAccessor.setValue(editor, "artist_", "new Artist");
        PrivilegedAccessor.setValue(editor, "genre_", "Classic Rock");
        PrivilegedAccessor.setValue(editor, "license_", "some license");
        
        int retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath());
        
    	assertEquals(0,retVal);

        //3. Test if the data was written correctly
        data = (AudioMetaData)MetaData.parse(TEST_FILE);

        assertEquals("Title not written", "New Title", data.getTitle());
        assertEquals("Artist not written", "new Artist",  data.getArtist());
        assertEquals("Genre not written", "Classic Rock", data.getGenre());
        assertEquals("Incorrect album","allAlbum", data.getAlbum());
        assertEquals("Incorrect year", "1234",data.getYear());
        assertEquals("Incorrect track", 3, data.getTrack());
        assertEquals("Incorrect comment", "allComment",data.getComment());
        assertEquals("License not written", "some license", data.getLicense());
    }
    
}
