package com.limegroup.gnutella.xml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NameValue;


public class CollectionTest extends com.limegroup.gnutella.util.BaseTestCase {

    FileDesc[] files = new FileDesc[3];
    final String fileLocation = "com/limegroup/gnutella/xml/";
    final File mason = CommonUtils.getResourceFile(fileLocation + "nullfile.null");
    final File test1 = CommonUtils.getResourceFile(fileLocation + "test1.mp3");
    final File test2 = CommonUtils.getResourceFile(fileLocation + "test2.mp3");
    final String audioSchemaURI = "http://www.limewire.com/schemas/audio.xsd";
    final String videoSchemaURI = "http://www.limewire.com/schemas/video.xsd";

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
        files = new FileDesc[3];
        assertTrue("Necessary file nullfile.null cannot be found!", mason.exists());
        assertTrue("Necessary file test1.mp3 cannot be found!", test1.exists());
        assertTrue("Necessary file test2.mp3 cannot be found!", test2.exists());
        
        Set urns;
        urns = calculateAndCacheURN(mason);
        files[0] = new FileDesc(mason, urns, 0);
        urns = calculateAndCacheURN(test1);
        files[1] = new FileDesc(test1, urns, 1);
        urns = calculateAndCacheURN(test2);
        files[2] = new FileDesc(test2, urns, 2);
    }

    public void testAudio() {
        clearDirectory();
        // test construction
        LimeXMLReplyCollection collection = new LimeXMLReplyCollection(audioSchemaURI);
        assertEquals(0, collection.getCount());
        for(int i = 0; i < files.length; i++)
            collection.initialize(files[i], Collections.EMPTY_LIST);
        assertEquals(0, collection.getCount());            
        for(int i = 0; i < files.length; i++)
            collection.createIfNecessary(files[i]);
        assertEquals("LimeXMLCollection count wrong!", 2, collection.getCount());

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

        assertEquals("Wrong keywords: " + keywords.toString(), 8, keywords.size());

        //Note: the Test1 and Test2 both contain the keyword "Movie", so we
        //check only 7 keywords while there are 8 entries
        assertTrue("Correct keywords not in map!", 
                          (keywords.contains("othertfield") && 
                           keywords.contains("otherafield")  &&
                           keywords.contains("Other")  &&
                           keywords.contains("tfield")  &&
                           keywords.contains("afield")  &&
                           keywords.contains("Vocal")  &&
                           keywords.contains("Movie") )
                          );
        
    }


    public void testVideo() {
        clearDirectory();
        // test construction
        LimeXMLReplyCollection collection = new LimeXMLReplyCollection(videoSchemaURI);
        assertEquals(0, collection.getCount());
        for(int i = 0; i < files.length; i++)
            collection.initialize(files[i], Collections.EMPTY_LIST);
        assertEquals(0, collection.getCount());            
        for(int i = 0; i < files.length; i++)
            collection.createIfNecessary(files[i]);
        assertEquals(0, collection.getCount());

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
        assertEquals("Wrong keyword count!", 0,  keywords.size());
    }


    
    public void testMatching() {
        clearDirectory();
        LimeXMLReplyCollection collection = new LimeXMLReplyCollection(audioSchemaURI);
        assertEquals(0, collection.getCount());
        for(int i = 0; i < files.length; i++)
            collection.initialize(files[i], Collections.EMPTY_LIST);
        assertEquals(0, collection.getCount());            
        for(int i = 0; i < files.length; i++)
            collection.createIfNecessary(files[i]);
        assertEquals("LimeXMLCollection count wrong!", 2, collection.getCount());

        // make sure that a simple match works....
        List nameVals = new ArrayList();
        nameVals.add(new NameValue(TITLE_KEY, "tfie"));
        LimeXMLDocument searchDoc = new LimeXMLDocument(nameVals, 
                                                        audioSchemaURI);
        List results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                     1, results.size());
                     
        // Ensure that a search for a matching title doesn't work
        // if we put it in the artist field ...
        nameVals = new ArrayList();
        nameVals.add(new NameValue(ARTIST_KEY, "tfie"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                     0, results.size());

        // make sure a little more complex match works, no mp3 has album but one
        // has a title beginning with s
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "susheel"));
        nameVals.add(new NameValue(TITLE_KEY, "o"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                         1, results.size());

        // has two matching, one null
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "tfield"));
        nameVals.add(new NameValue(ALBUM_KEY, "ignored"));
        nameVals.add(new NameValue(ARTIST_KEY, "afield"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                     1, results.size());

        // has two matching
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "othertf"));
        nameVals.add(new NameValue(ARTIST_KEY, "otherafi"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results", 
                          1, results.size()); 

        // has one off by one....
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "othert"));
        nameVals.add(new NameValue(ARTIST_KEY, "othra"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                          0, results.size());

        // has all nulls, this was a terrible bug in previous versions...
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "swi"));
        nameVals.add(new NameValue(TRACK_KEY, "ferb"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                        0, results.size());

        // has all nulls, this was a terrible bug in previous versions...
        nameVals.clear();
        nameVals.add(new NameValue(TRACK_KEY, "ferb"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                         0,  results.size());
                         
        // test matching on numeric value ...
        nameVals.clear();
        nameVals.add(new NameValue(BITRATE_KEY, "64"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                        2, results.size());
                        
        // test things don't match on partial numeric value...
        nameVals.clear();
        nameVals.add(new NameValue(BITRATE_KEY, "6"));
        searchDoc = new LimeXMLDocument(nameVals, audioSchemaURI);
        results = collection.getMatchingReplies(searchDoc);
        assertEquals("Not the correct amount of results",
                        0, results.size());
    }


    public void testSerialized()  throws Exception  {
        createFiles();
        populateDirectory();

        LimeXMLReplyCollection audioCollection = new LimeXMLReplyCollection(audioSchemaURI);
        LimeXMLReplyCollection videoCollection = new LimeXMLReplyCollection(videoSchemaURI);
        assertEquals(0, audioCollection.getCount());
        assertEquals(0, videoCollection.getCount());
        
        for(int i = 0; i < files.length; i++)
            audioCollection.initialize(files[i], Collections.EMPTY_LIST);
        assertEquals("LimeXMLCollection count wrong!", 2, audioCollection.getCount());
        
        for(int i = 0; i < files.length; i++)
            videoCollection.initialize(files[i], Collections.EMPTY_LIST);
        assertEquals("LimeXMLCollection count wrong!", 2, videoCollection.getCount());

        // test assocation
        assertNull("Mason should not have a doc!", audioCollection.getDocForHash(getHash(mason)));
        assertNotNull("Test1 should have a doc!", audioCollection.getDocForHash(getHash(test1)));
        assertNotNull("Test2 should have a doc!", audioCollection.getDocForHash(getHash(test2)));
        assertNotNull("Mason should have a doc!", videoCollection.getDocForHash(getHash(mason)));
        assertNull("Test1 should not have a doc!", videoCollection.getDocForHash(getHash(test1)));
        assertNotNull("Test2 should have a doc!", videoCollection.getDocForHash(getHash(test2)));


        // test keyword generation
        List keywords = audioCollection.getKeyWords();
        assertEquals("Wrong keyword count!", 8, keywords.size());
        assertTrue("Correct keywords not in map!", 
                          (keywords.contains("othertfield") && 
                           keywords.contains("otherafield")  &&
                           keywords.contains("Other")  &&
                           keywords.contains("tfield")  &&
                           keywords.contains("afield")  &&
                           keywords.contains("Vocal") &&
                           keywords.contains("Movie") )
                          );
        keywords = videoCollection.getKeyWords();
        assertEquals("Wrong keyword count!", 4, keywords.size());
        assertTrue("Correct keywords not in map!", 
                          (keywords.contains("null") && 
                           keywords.contains("file")  &&
                           keywords.contains("susheel")  &&
                           keywords.contains("daswani") )
                          );
        
        clearDirectory();
    }

    //test LimeXMLReplyCollection with i18n chars
    public void testI18NCharsInXML() throws Exception {
        String[] keywds = {"\u9234\u6728\u6b66\u8535\u69d8",
                           "thestudio",
                           "\u5bae\u672c"};

        String dir1 = "director=\"" + keywds[0] + "\"";
        String dir2 = dir1 + " studio=\"" + keywds[1] + "\"";
        String dir3 = dir1 + " studio=\"" + keywds[2] + "\"";


        LimeXMLReplyCollection collection = new LimeXMLReplyCollection(videoSchemaURI);
        LimeXMLDocument newDoc = new LimeXMLDocument(buildXMLString(dir2));
        LimeXMLDocument newDoc2 = new LimeXMLDocument(buildXMLString(dir3));
        
        //addReply calls addKeywords (which uses I18NConvert)
        collection.addReply(files[0], newDoc);
        collection.addReply(files[1], newDoc2);

        //check the keywords.
        List l = collection.getKeyWords();
        assertTrue("Couldn't find all keywords",
                   (l.contains(keywds[0]) &&
                    l.contains(keywds[1]) &&
                    l.contains(keywds[2])));

        //the query document
        LimeXMLDocument newDocQ = new LimeXMLDocument(buildXMLString(dir1));
        LimeXMLDocument newDocQ2 = new LimeXMLDocument(buildXMLString(dir3));

        //make sure we get matches that we expect
        l = collection.getMatchingReplies(newDocQ);
        assertEquals("should of found two matches", 2, l.size());
        
        l = collection.getMatchingReplies(newDocQ2);
        assertEquals("should of found only one match", 1, l.size());
        
        //make sure we get the same xml string...
        assertEquals("didn't get expected xml string",
                     newDoc2.getXMLString(), ((LimeXMLDocument)l.iterator().next()).getXMLString());


        //check we get the right docs back from getDocForHash
        LimeXMLDocument returnDoc = collection.getDocForHash(files[0].getSHA1Urn());
        assertEquals("didn't get expected xml string (getDocForHash)",
                     newDoc.getXMLString(), returnDoc.getXMLString());
        
        returnDoc = collection.getDocForHash(files[1].getSHA1Urn());
        assertEquals("didn't get expected xml string (getDocForHash)",
                     newDoc2.getXMLString(), returnDoc.getXMLString());
        

        //remove 
        collection.removeDoc(files[1]);
        
        l = collection.getMatchingReplies(newDocQ2);
        assertEquals("should not of found a match", 0, l.size());
        
        //make sure the keywords got deleted as well
        l = collection.getKeyWords();
        assertTrue("unexpected keywords",
                   l.contains(keywds[0]) && l.contains(keywds[1]) && !l.contains(keywds[2]));
    }

    // build xml string for video
    private String buildXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video " 
            + keyname 
            + "></video></videos>";
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
        // make audio.collection
        doc = MetaDataReader.readDocument(test1);
        map.put(getHash(test1), doc);
        doc = MetaDataReader.readDocument(test2);
        map.put(getHash(test2), doc);
        File marker = CommonUtils.getResourceFile(fileLocation + "nullfile.null");
        marker = marker.getParentFile();
        write(map, new File(marker, "audio.collection"));
        
        // made video.collection
        List nameVals = new ArrayList();
        map = new HashMap();
        nameVals.add(new NameValue("videos__video__director__","daswani"));
        nameVals.add(new NameValue("videos__video__title__","susheel"));
        doc = new LimeXMLDocument(nameVals, videoSchemaURI);
        doc.initIdentifier(test2);
        doc.getXMLString();
        map.put(getHash(test2), doc);
        nameVals = new ArrayList();
        nameVals.add(new NameValue("videos__video__director__","file"));
        nameVals.add(new NameValue("videos__video__title__","null"));
        doc = new LimeXMLDocument("<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video title=\"null\" director=\"file\" ></video></videos>");
        doc.initIdentifier(mason);
        doc.getXMLString();
        map.put(getHash(mason), doc);
        write(map, new File(marker, "video.collection"));
    }
    
    private void write(Object o, File f) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        out.writeObject(o);
        out.flush();
        out.close();
    }

    private void populateDirectory() {
        File audioFile = CommonUtils.getResourceFile(fileLocation + "audio.collection");
        File videoFile = CommonUtils.getResourceFile(fileLocation + "video.collection");
        File newAudio  = new File(LimeXMLProperties.instance().getXMLDocsDir(), "audio.sxml");
        File newVideo  = new File(LimeXMLProperties.instance().getXMLDocsDir(), "video.sxml");
        assertTrue("Necessary file audio.collection cannot be found!", audioFile.exists());
        assertTrue("Necessary file video.collection cannot be found!", videoFile.exists());
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



