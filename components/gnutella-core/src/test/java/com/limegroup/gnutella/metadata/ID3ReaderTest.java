package com.limegroup.gnutella.metadata;


import java.io.File;
import java.io.IOException;

import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class ID3ReaderTest extends LimeTestCase {

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
        String dir = "com/limegroup/gnutella/metadata/";
        TEST_FILE = CommonUtils.getResourceFile(dir+"ID3TestFile.mp3");
        assertTrue("file should exist", TEST_FILE.exists());
    }
    
    //////////

    /**
     * Tests that the ID3v2 tags are read correctly
     */
    public void testID3v2Tags() throws Exception {
        //ID3Reader.ID3Data data = null;
        
        //data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
          //     ID3Reader.class, "parseID3v2Data", new Object[] { TEST_FILE });

    	MP3MetaData data = new OnlyID3v2MetaData(TEST_FILE);
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
        //ID3Reader.ID3Data data = null;
        
        //data = (ID3Reader.ID3Data)PrivilegedAccessor.invokeMethod(
          //     ID3Reader.class, "parseFile", new Object[] { TEST_FILE });

        MP3MetaData data = (MP3MetaData) MetaData.parse(TEST_FILE);
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
    
    class OnlyID3v2MetaData extends MP3MetaData {
    	public OnlyID3v2MetaData(File f) throws IOException{
    		super(f);
    	}
    	
    	protected void parseFile(File f) throws IOException{
    		try {
    			PrivilegedAccessor.invokeMethod(this,"parseID3v2Data", f);
    		}catch(Exception ex){
    			//have to cast because privileged accessor does not declare IOX
    			if (ex instanceof IOException)
    				throw (IOException)ex;  
    			ex.printStackTrace();
    		}
    	}
    }
}
