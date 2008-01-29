package com.limegroup.gnutella.metadata;

import java.io.File;
import java.util.List;

import junit.framework.Test;

import org.limewire.collection.NameValue;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactoryImpl;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class OGGWriterTest extends AudioTest {

    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    private String newTitle = "new title";
    private String newArtist = "new artist";
    private String newAlbum = "new album";
    
    public OGGWriterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OGGWriterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp(){
        Injector injector = LimeTestUtils.createInjector();
        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactoryImpl.class);
    }
    
    /**
     * Tests writing meta-data to an Ogg File
     */
    public void testOGGTagsWriting() throws Exception {
        
        File f = CommonUtils.getResourceFile(dir + "oggAll.ogg");
        assertTrue(f.exists());
        File TEST_FILE = new File("test0012321.ogg");
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
        
        
        // read the meta data from the current audio file
        MetaReader data = MetaDataFactory.parse(TEST_FILE);
        // test the contents to be sure its valid
        testTag((AudioMetaData) data.getMetaData());
        
        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
 
        //2. Write data into the file 
        MetaWriter editor = MetaDataFactory.getEditorForFile(TEST_FILE.getCanonicalPath());
        editor.populate(doc);

        int retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath());
        
        assertEquals(0,retVal);
        
        // read the file again
        data = MetaDataFactory.parse(TEST_FILE);
        AudioMetaData amd = (AudioMetaData) data.getMetaData();
        
        // test the values, chanages values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }

}