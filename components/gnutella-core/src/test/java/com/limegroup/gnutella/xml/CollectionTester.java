package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.mp3.*;
import junit.framework.*;

/** Unit test for LimeXMLReplyCollection.
 * Should run in the tests/xml directory (ie where this file is located).
 */
public class CollectionTester extends TestCase {

    final Map files = new HashMap();
    final String fileLocation = "com/limegroup/gnutella/xml/";
    final File mason = new File(fileLocation + "nullfile.null");
    final File test1 = new File(fileLocation + "test1.mp3");
    final File test2 = new File(fileLocation + "test2.mp3");
    final String schemaURI = "http://www.limewire.com/schemas/audio.xsd";
    final String schemaURIVideo = "http://www.limewire.com/schemas/video.xsd";
    final MetaFileManager mfm = new MFMStub();
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
        

    public CollectionTester(String name) {
        super(name);
    }
    
    protected void setUp() {
        if (!mason.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        if (!test1.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        if (!test2.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        files.clear();
        Assert.assertTrue("Necessary file nullfile.null cannot be found!",
                          mason.exists());
        Assert.assertTrue("Necessary file test1.mp3 cannot be found!",
                          test1.exists());
        Assert.assertTrue("Necessary file test2.mp3 cannot be found!",
                          test2.exists());
        files.put(mason, mfm.readFromMap(mason));
        files.put(test1, mfm.readFromMap(test1));
        files.put(test2, mfm.readFromMap(test2));
    }

    public void testAudio() {
        clearDirectory();
        // test construction
        LimeXMLReplyCollection collection = 
        new LimeXMLReplyCollection(files, schemaURI, mfm, audio);
        Assert.assertTrue("LimeXMLCollection count wrong!  Count is " + 
                          collection.getCount(),
                          (collection.getCount() == 2));

        // test assocation
        LimeXMLDocument doc = null;
        doc = collection.getDocForHash(mfm.readFromMap(mason));
        Assert.assertTrue("Mason should not have a doc!",
                          doc == null);
        doc = collection.getDocForHash(mfm.readFromMap(test1));
        Assert.assertTrue("Test1 should have a doc!",
                          doc != null);
        doc = collection.getDocForHash(mfm.readFromMap(test2));
        Assert.assertTrue("Test2 should have a doc!",
                          doc != null);

        // test keyword generation
        List keywords = collection.getKeyWords();
        Assert.assertTrue("Keywords should have 6, instead " + keywords.size(),
                          (keywords.size() == 6));
        Assert.assertTrue("Correct keywords not in map!", 
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
        new LimeXMLReplyCollection(files, schemaURIVideo, mfm, !audio);
        Assert.assertTrue("LimeXMLCollection count wrong!  Count is " + 
                          collection.getCount(),
                          (collection.getCount() == 0));

        // test assocation
        LimeXMLDocument doc = null;
        doc = collection.getDocForHash(mfm.readFromMap(mason));
        Assert.assertTrue("Mason should not have a doc!",
                          doc == null);
        doc = collection.getDocForHash(mfm.readFromMap(test1));
        Assert.assertTrue("Test1 should not have a doc!",
                          doc == null);
        doc = collection.getDocForHash(mfm.readFromMap(test2));
        Assert.assertTrue("Test2 should not have a doc!",
                          doc == null);

        // test keyword generation
        List keywords = collection.getKeyWords();
        Assert.assertTrue("Keywords should have 4, instead " + keywords.size(),
                          (keywords.size() == 0));
    }


    
    public void testMatching() {
        clearDirectory();
        LimeXMLReplyCollection collection = 
        new LimeXMLReplyCollection(files, schemaURI, mfm, audio);

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
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));        

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
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));        

        // has one off by one....
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "othert"));
        nameVals.add(new NameValue(ARTIST_KEY, "othra"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 0));        

        // has all nulls, this was a terrible bug in previous versions...
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "swi"));
        nameVals.add(new NameValue(TRACK_KEY, "ferb"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 0));        

        // has all nulls, this was a terrible bug in previous versions...
        nameVals.clear();
        nameVals.add(new NameValue(TRACK_KEY, "ferb"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 0));        

    }


    public void testSerialized() {
        createFiles();
        populateDirectory();

        LimeXMLReplyCollection audioCollection = 
        new LimeXMLReplyCollection(files, schemaURI, mfm, audio);
        LimeXMLReplyCollection videoCollection = 
        new LimeXMLReplyCollection(files, schemaURIVideo, mfm, !audio);

        // test it got the count right....        
        Assert.assertTrue("LimeXMLCollection count wrong!  Count is " + 
                          audioCollection.getCount(),
                          (audioCollection.getCount() == 2));
        Assert.assertTrue("LimeXMLCollection count wrong!  Count is " + 
                          videoCollection.getCount(),
                          (videoCollection.getCount() == 2));

        // test assocation
        LimeXMLDocument doc = null;
        doc = audioCollection.getDocForHash(mfm.readFromMap(mason));
        Assert.assertTrue("Mason should not have a doc!",
                          doc == null);
        doc = audioCollection.getDocForHash(mfm.readFromMap(test1));
        Assert.assertTrue("Test1 should have a doc!",
                          doc != null);
        doc = audioCollection.getDocForHash(mfm.readFromMap(test2));
        Assert.assertTrue("Test2 should have a doc!",
                          doc != null);
        doc = videoCollection.getDocForHash(mfm.readFromMap(mason));
        Assert.assertTrue("Mason should have a doc!",
                          doc != null);
        doc = videoCollection.getDocForHash(mfm.readFromMap(test1));
        Assert.assertTrue("Test1 should not have a doc!",
                          doc == null);
        doc = videoCollection.getDocForHash(mfm.readFromMap(test2));
        Assert.assertTrue("Test2 should have a doc!",
                          doc != null);


        // test keyword generation
        List keywords = audioCollection.getKeyWords();
        Assert.assertTrue("Keywords should have 6, instead " + keywords.size(),
                          (keywords.size() == 6));
        Assert.assertTrue("Correct keywords not in map!", 
                          (keywords.contains("othertfield") && 
                           keywords.contains("otherafield")  &&
                           keywords.contains("Other")  &&
                           keywords.contains("tfield")  &&
                           keywords.contains("afield")  &&
                           keywords.contains("Vocal") )
                          );
        keywords = videoCollection.getKeyWords();
        Assert.assertTrue("Keywords should have 4, instead " + keywords.size(),
                          (keywords.size() == 4));
        Assert.assertTrue("Correct keywords not in map!", 
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


    private void createFiles() {
        try {
        HashMap map = new HashMap();
        LimeXMLDocument doc = null;
        LimeXMLReplyCollection.MapSerializer ms = null;
        // make audio.collection
        doc = (new ID3Reader()).readDocument(test1);
        map.put(mfm.readFromMap(test1), doc);
        doc = (new ID3Reader()).readDocument(test2);
        map.put(mfm.readFromMap(test2), doc);
        ms = new LimeXMLReplyCollection.MapSerializer(new File(fileLocation+"audio.collection"), map);
        ms.commit();
        // made video.collection
        List nameVals = new ArrayList();
        map = new HashMap();
        nameVals.add(new NameValue("videos__video__director__","daswani"));
        nameVals.add(new NameValue("videos__video__title__","susheel"));
        doc = new LimeXMLDocument(nameVals, schemaURIVideo);
        doc.setIdentifier(test2.getCanonicalPath());
        doc.getXMLString();
        map.put(mfm.readFromMap(test2), doc);
        nameVals = new ArrayList();
        nameVals.add(new NameValue("videos__video__director__","file"));
        nameVals.add(new NameValue("videos__video__title__","null"));
        doc = new LimeXMLDocument("<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video title=\"null\" director=\"file\" ></video></videos>");
        doc.setIdentifier(mason.getCanonicalPath());
        doc.getXMLString();
        map.put(mfm.readFromMap(mason), doc);
        ms = new LimeXMLReplyCollection.MapSerializer(new File(fileLocation+"video.collection"), map);
        ms.commit();
        }
        catch (Exception e) {
            Assert.assertTrue("Could not make collections!",
                              false);
        }
    }
    

    private void populateDirectory() {
        File audioFile = new File(fileLocation + "audio.collection");
        File videoFile = new File(fileLocation + "video.collection");
        File newAudio  = new File("lib/xml/data/audio.sxml");
        File newVideo  = new File("lib/xml/data/video.sxml");
        Assert.assertTrue("Necessary file audio.collection cannot be found!",
                          audioFile.exists());
        Assert.assertTrue("Necessary file video.collection cannot be found!",
                          videoFile.exists());
        audioFile.renameTo(newAudio);
        videoFile.renameTo(newVideo);
    }


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite =  new TestSuite("LimeXMLReplyCollection Unit Test");
        suite.addTest(new TestSuite(CollectionTester.class));
        return suite;
    }

}
