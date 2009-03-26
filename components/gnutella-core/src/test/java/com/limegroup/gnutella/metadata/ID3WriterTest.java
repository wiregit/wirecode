package com.limegroup.gnutella.metadata;

import java.io.File;
import java.util.List;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.NameValue;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

public class ID3WriterTest extends AudioTestBase {

    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private MetaDataFactory metaDataFactory;
    
    private String newTitle = "new title";
    private String newArtist = "new artist";
    private String newAlbum = "new album";
    
    public ID3WriterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ID3WriterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp(){
        Injector injector = LimeTestUtils.createInjector();
        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        metaDataFactory = injector.getInstance(MetaDataFactory.class);
    }
    
    /**
     * Tests writing meta-data to an FLAC File
     */
    public void testID3v1TagsWriting() throws Exception {
        
        File f = TestUtils.getResourceFile(dir + "ID3V1.mp3");
        assertTrue(f.exists());
        File TEST_FILE = new File("test0012321.mp3");
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
        
        
        // read the meta data from the current audio file
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        // test the contents to be sure its valid
        validateTag(data);
        
        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
 
        //2. Write data into the file 
        MetaWriter editor = metaDataFactory.getEditorForFile(TEST_FILE.getCanonicalPath());
        AudioMetaData audioMetaData = new AudioMetaData();
        audioMetaData.populate(doc);

        MetaDataState retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath(), audioMetaData);
        
        assertEquals(MetaDataState.NORMAL,retVal);
        
        // read the file again
        data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        AudioMetaData amd = data;
        
        // test the values, changes values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }
    
    /**
     * Tests writing meta-data to an FLAC File
     */
    public void testID3v11TagsWriting() throws Exception {
        
        File f = TestUtils.getResourceFile(dir + "ID3V11.mp3");
        assertTrue(f.exists());
        File TEST_FILE = new File("test0012321.mp3");
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
        
        
        // read the meta data from the current audio file
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        // test the contents to be sure its valid
        validateTag(data);
        
        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
 
        //2. Write data into the file 
        MetaWriter editor = metaDataFactory.getEditorForFile(TEST_FILE.getCanonicalPath());
        AudioMetaData audioMetaData = new AudioMetaData();
        audioMetaData.populate(doc);

        MetaDataState retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath(), audioMetaData);
        
        assertEquals(MetaDataState.NORMAL,retVal);
        
        // read the file again
        data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        AudioMetaData amd = data;
        
        // test the values, changes values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }
    
    /**
     * Tests writing meta-data to an FLAC File
     */
    public void testID3v22TagsWriting() throws Exception {
        
        File f = TestUtils.getResourceFile(dir + "ID3V22.mp3");
        assertTrue(f.exists());
        File TEST_FILE = new File("test0012321.mp3");
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
        
        // read the meta data from the current audio file
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        // test the contents to be sure its valid
        validateTag(data);
        
        
        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
 
        //2. Write data into the file 
        MetaWriter editor = metaDataFactory.getEditorForFile(TEST_FILE.getCanonicalPath());
        AudioMetaData audioMetaData = new AudioMetaData();
        audioMetaData.populate(doc);

        MetaDataState retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath(), audioMetaData);
        
        assertEquals(MetaDataState.NORMAL,retVal);
        
        // read the file again
        data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        AudioMetaData amd = data;
        
        // test the values, changes values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }
    
    /**
     * Tests writing meta-data to an FLAC File
     */
    public void testID3v23TagsWriting() throws Exception {
        
        File f = TestUtils.getResourceFile(dir + "ID3V23.mp3");
        assertTrue(f.exists());
        File TEST_FILE = new File("test0012321.mp3");
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
        
        // read the meta data from the current audio file
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        // test the contents to be sure its valid
        validateTag(data);
        

        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
 
        //2. Write data into the file 
        MetaWriter editor = metaDataFactory.getEditorForFile(TEST_FILE.getCanonicalPath());
        AudioMetaData audioMetaData = new AudioMetaData();
        audioMetaData.populate(doc);

        MetaDataState retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath(), audioMetaData);
        
        assertEquals(MetaDataState.NORMAL,retVal);
        
        // read the file again
        data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        AudioMetaData amd = data;
        
        // test the values, changes values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }
    
    /**
     * Tests writing meta-data to an FLAC File
     */
    public void testID3v24TagsWriting() throws Exception {
        
        File f = TestUtils.getResourceFile(dir + "ID3V24.mp3");
        assertTrue(f.exists());
        File TEST_FILE = new File("test0012321.mp3");
        TEST_FILE.delete();
        FileUtils.copy(f, TEST_FILE);
        assertTrue(TEST_FILE.exists());
        TEST_FILE.deleteOnExit();
        
        // read the meta data from the current audio file
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        // test the contents to be sure its valid
        validateTag(data);
    
        
        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
 
        //2. Write data into the file 
        MetaWriter editor = metaDataFactory.getEditorForFile(TEST_FILE.getCanonicalPath());
        AudioMetaData audioMetaData = new AudioMetaData();
        audioMetaData.populate(doc);

        MetaDataState retVal = editor.commitMetaData(TEST_FILE.getAbsolutePath(), audioMetaData);
        
        assertEquals(MetaDataState.NORMAL,retVal);
        
        // read the file again
        data = (AudioMetaData) metaDataFactory.parse(TEST_FILE);
        AudioMetaData amd = data;
              
        // test the values, changes values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }

}
