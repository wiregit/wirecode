package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.mp3.*;
import junit.framework.*;

/** Unit test for LimeXMLReplyCollection.
 * Should run in the tests/xml directory (ie where this file is located).
 */
public class CollectionTest extends com.limegroup.gnutella.util.BaseTestCase {

    FileDesc[] files = new FileDesc[3];
    final String fileLocation = "com/limegroup/gnutella/xml/";
    final File mason = CommonUtils.getResourceFile(fileLocation + "nullfile.null");
    final File test1 = CommonUtils.getResourceFile(fileLocation + "test1.mp3");
    final File test2 = CommonUtils.getResourceFile(fileLocation + "test2.mp3");
    final String schemaURI = "http://www.limewire.com/schemas/audio.xsd";
    final String schemaURIVideo = "http://www.limewire.com/schemas/video.xsd";
    final boolean audio = true;

    private final String KEY_PREFIX = "audios" + XMLStringUtils.DELIMITER +
        "audio" + XMLStringUtils.DELIMITER;

    private final String ARTIST_KEY =   KEY_PREFIX + "artist" + 
        XMLStringUtils.DELIMITER;
    private final String ALBUM_KEY =    KEY_PREFIX + "album" + 
        XMLStringUtils.DELIMITER;
    private final String TITLE_KEY =    KEY_PREFIX + "title" + 
        XMLStringUtils.DELIMITER;
    private final String TRACK_KEY =    KEY_PREFIX + "track" + 
        XMLStringUtils.DELIMITER;
    private final String BITRATE_KEY =  KEY_PREFIX + "bitrate" +
        XMLStringUtils.DELIMITER;
        

    public CollectionTest(String name) {
        super(name);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(CollectionTest.class);
    }
    
    protected void setUp() throws Exception {
        
        Expand.expandFile( 
            CommonUtils.getResourceFile("com/limegroup/gnutella/xml/xml.war"), 
            CommonUtils.getUserSettingsDir()
                         );
        
        if (!mason.exists())
            assertTrue("Could not find necessary file!", false);
        if (!test1.exists())
            assertTrue("Could not find necessary file!", false);
        if (!test2.exists())
            assertTrue("Could not find necessary file!", false);
        files = new FileDesc[3];
        assertTrue("Necessary file nullfile.null cannot be found!",
                          mason.exists());
        assertTrue("Necessary file test1.mp3 cannot be found!",
                          test1.exists());
        assertTrue("Necessary file test2.mp3 cannot be found!",
                          test2.exists());
        Set urns;
        urns = FileDesc.calculateAndCacheURN(mason);
        files[0] = new FileDesc(mason, urns, 0);
        urns = FileDesc.calculateAndCacheURN(test1);
        files[1] = new FileDesc(test1, urns, 1);
        urns = FileDesc.calculateAndCacheURN(test2);
        files[2] = new FileDesc(test2, urns, 2);
    }

    public void testAudio() {
        clearDirectory();
        // test construction
        LimeXMLReplyCollection collection = 
            new LimeXMLReplyCollection(files, schemaURI, audio);
        assertEquals("LimeXMLCollection count wrong!",
                         2, collection.getCount());

        // test assocation
        LimeXMLDocument doc = null;
        doc = collection.getDocForHash(getHash(mason));
        assertNull("Mason should not have a doc!", doc);
        doc = collection.getDocForHash(getHash(test1));
        assertNotNull("Test1 should have a doc!", doc);
        doc = collection.getDocForHash(getHash(test2));
        assertNotNull("Test2 should have a doc!", doc);

        // test keyword generation
        List keywords = collection.getKeyWords();
        assertEquals("Wrong keyword count!",
                         6, keywords.size());
        assertTrue("Correct keywords not in map!", 
                          (keywords.contains("othertfield") && 
                           keywords.contains("otherafield")  &&
                           keywords.contains("Other")  &&
                           keywords.contains("tfield")  &&
                           keywords.contains("afield")  &&
                           keywords.contains("Vocal") )
                          );
        
    }


    public void testVideo() {
        clearDirectory();
        // test construction
        LimeXMLReplyCollection collection = 
            new LimeXMLReplyCollection(files, schemaURIVideo, !audio);
        assertEquals("LimeXMLCollection count wrong!",
                         0, collection.getCount());

        // test assocation
        LimeXMLDocument doc = null;
        doc = collection.getDocForHash(getHash(mason));
        assertNull("Mason should not have a doc!", doc);
        doc = collection.getDocForHash(getHash(test1));
        assertNull("Test1 should not have a doc!", doc);
        doc = collection.getDocForHash(getHash(test2));
        assertNull("Test2 should not have a doc!", doc);

        // test keyword generation
        List keywords = collection.getKeyWords();
        assertEquals("Wrong keyword count!",
                        0,  keywords.size());
    }


    
    public void testMatching() {
        clearDirectory();
        LimeXMLReplyCollection collection = 
            new LimeXMLReplyCollection(files, schemaURI, audio);

        // make sure that a simple match works....
        List nameVals = new ArrayList();
        nameVals.add(new NameValue(TITLE_KEY, "tfie"));
        LimeXMLDocument searchDoc = new LimeXMLDocument(nameVals, 
                                                        schemaURI);
        List results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                     1, results.size());

        // make sure a little more complex match works, no mp3 has album but one
        // has a title beginning with s
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "susheel"));
        nameVals.add(new NameValue(TITLE_KEY, "o"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                         1, results.size());

        // has two matching, one null
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "tfield"));
        nameVals.add(new NameValue(ALBUM_KEY, "ignored"));
        nameVals.add(new NameValue(ARTIST_KEY, "afield"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                     1, results.size());

        // has two matching
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "othertf"));
        nameVals.add(new NameValue(ARTIST_KEY, "otherafi"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results", 
                          1, results.size());

        // has one off by one....
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "othert"));
        nameVals.add(new NameValue(ARTIST_KEY, "othra"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                          0, results.size());

        // has all nulls, this was a terrible bug in previous versions...
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "swi"));
        nameVals.add(new NameValue(TRACK_KEY, "ferb"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                        0, results.size());

        // has all nulls, this was a terrible bug in previous versions...
        nameVals.clear();
        nameVals.add(new NameValue(TRACK_KEY, "ferb"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                         0,  results.size());
                         
        // test matching on numeric value ...
        nameVals.clear();
        nameVals.add(new NameValue(BITRATE_KEY, "64"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                        2, results.size());
                        
        // test things don't match on partial numeric value...
        nameVals.clear();
        nameVals.add(new NameValue(BITRATE_KEY, "6"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                        0, results.size());
    }


    public void testSerialized()  throws Exception  {
        createFiles();
        populateDirectory();

        LimeXMLReplyCollection audioCollection = 
            new LimeXMLReplyCollection(files, schemaURI, audio);
        LimeXMLReplyCollection videoCollection = 
            new LimeXMLReplyCollection(files, schemaURIVideo, !audio);

        // test it got the count right....        
        assertEquals("LimeXMLCollection count wrong!",
                         2, audioCollection.getCount());
        assertEquals("LimeXMLCollection count wrong!",
                          2, videoCollection.getCount());

        // test assocation
        LimeXMLDocument doc = null;
        doc = audioCollection.getDocForHash(getHash(mason));
        assertNull("Mason should not have a doc!", doc);
        doc = audioCollection.getDocForHash(getHash(test1));
        assertNotNull("Test1 should have a doc!", doc);
        doc = audioCollection.getDocForHash(getHash(test2));
        assertNotNull("Test2 should have a doc!", doc);
        doc = videoCollection.getDocForHash(getHash(mason));
        assertNotNull("Mason should have a doc!", doc);
        doc = videoCollection.getDocForHash(getHash(test1));
        assertNull("Test1 should not have a doc!", doc);
        doc = videoCollection.getDocForHash(getHash(test2));
        assertNotNull("Test2 should have a doc!", doc);


        // test keyword generation
        List keywords = audioCollection.getKeyWords();
        assertEquals("Wrong keyword count!",
                         6, keywords.size());
        assertTrue("Correct keywords not in map!", 
                          (keywords.contains("othertfield") && 
                           keywords.contains("otherafield")  &&
                           keywords.contains("Other")  &&
                           keywords.contains("tfield")  &&
                           keywords.contains("afield")  &&
                           keywords.contains("Vocal") )
                          );
        keywords = videoCollection.getKeyWords();
        assertEquals("Wrong keyword count!",
                         4, keywords.size());
        assertTrue("Correct keywords not in map!", 
                          (keywords.contains("null") && 
                           keywords.contains("file")  &&
                           keywords.contains("susheel")  &&
                           keywords.contains("daswani") )
                          );
        
        clearDirectory();
    }


    private void clearDirectory() {
        File dir = new File(LimeXMLProperties.instance().getXMLDocsDir());
        if (dir.exists() && dir.isDirectory()) {
            // clear the files in the directory....
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++)
                files[i].delete();
        }
    }


    private void createFiles() throws Exception {
        HashMap map = new HashMap();
        LimeXMLDocument doc = null;
        LimeXMLReplyCollection.MapSerializer ms = null;
        // make audio.collection
        doc = ID3Reader.readDocument(test1);
        map.put(getHash(test1), doc);
        doc = ID3Reader.readDocument(test2);
        map.put(getHash(test2), doc);
        ms = new LimeXMLReplyCollection.MapSerializer(CommonUtils.getResourceFile(fileLocation+"audio.collection"), map);
        ms.commit();
        // made video.collection
        List nameVals = new ArrayList();
        map = new HashMap();
        nameVals.add(new NameValue("videos__video__director__","daswani"));
        nameVals.add(new NameValue("videos__video__title__","susheel"));
        doc = new LimeXMLDocument(nameVals, schemaURIVideo);
        doc.setIdentifier(test2.getCanonicalPath());
        doc.getXMLString();
        map.put(getHash(test2), doc);
        nameVals = new ArrayList();
        nameVals.add(new NameValue("videos__video__director__","file"));
        nameVals.add(new NameValue("videos__video__title__","null"));
        doc = new LimeXMLDocument("<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video title=\"null\" director=\"file\" ></video></videos>");
        doc.setIdentifier(mason.getCanonicalPath());
        doc.getXMLString();
        map.put(getHash(mason), doc);
        ms = new LimeXMLReplyCollection.MapSerializer(CommonUtils.getResourceFile(fileLocation+"video.collection"), map);
        ms.commit();
    }
    

    private void populateDirectory() {
        File audioFile = CommonUtils.getResourceFile(fileLocation + "audio.collection");
        File videoFile = CommonUtils.getResourceFile(fileLocation + "video.collection");
        File newAudio  = new File(LimeXMLProperties.instance().getXMLDocsDir(), "audio.sxml");
        File newVideo  = new File(LimeXMLProperties.instance().getXMLDocsDir(), "video.sxml");
        assertTrue("Necessary file audio.collection cannot be found!",
                          audioFile.exists());
        assertTrue("Necessary file video.collection cannot be found!",
                          videoFile.exists());
        audioFile.renameTo(newAudio);
        videoFile.renameTo(newVideo);
    }
    
    private static URN getHash(File f) {
        try {
            return URN.createSHA1Urn(f);
        } catch(IOException ioe) {
            return null;
        } catch(InterruptedException ie) {
            return null;
        }
    }       
}
