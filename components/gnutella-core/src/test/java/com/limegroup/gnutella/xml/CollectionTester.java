package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;
import junit.framework.*;

/** Unit test for LimeXMLReplyCollection.
 * Should run in the tests/xml directory (ie where this file is located).
 */
public class CollectionTester extends TestCase {

    final Set files = new HashSet();
    final File mason = new File("nullfile.null");
    final File vader = new File("vader.mp3");
    final File swing = new File("swing.mp3");
    final String schemaURI =  "http://www.limewire.com/schemas/audio.xsd";
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
        if (!vader.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        if (!swing.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        files.clear();
        files.add(mason);
        files.add(vader);
        files.add(swing);
    }

    public void testAudio() {
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
        doc = collection.getDocForHash(mfm.readFromMap(vader));
        Assert.assertTrue("Vader should have a doc!",
                          doc != null);
        doc = collection.getDocForHash(mfm.readFromMap(swing));
        Assert.assertTrue("Swing should have a doc!",
                          doc != null);

        // test keyword generation
        List keywords = collection.getKeyWords();
        Assert.assertTrue("Keywords should have 4, instead " + keywords.size(),
                          (keywords.size() == 4));
        Assert.assertTrue("Correct keywords not in map!", 
                          (keywords.contains("ferris") && 
                           keywords.contains("swing")  &&
                           keywords.contains("darth")  &&
                           keywords.contains("vader") )
                          );
        
    }


    public void testVideo() {
        // test construction
        LimeXMLReplyCollection collection = 
        new LimeXMLReplyCollection(files, schemaURI, mfm, !audio);
        Assert.assertTrue("LimeXMLCollection count wrong!  Count is " + 
                          collection.getCount(),
                          (collection.getCount() == 0));

        // test assocation
        LimeXMLDocument doc = null;
        doc = collection.getDocForHash(mfm.readFromMap(mason));
        Assert.assertTrue("Mason should not have a doc!",
                          doc == null);
        doc = collection.getDocForHash(mfm.readFromMap(vader));
        Assert.assertTrue("Vader should not have a doc!",
                          doc == null);
        doc = collection.getDocForHash(mfm.readFromMap(swing));
        Assert.assertTrue("Swing should not have a doc!",
                          doc == null);

        // test keyword generation
        List keywords = collection.getKeyWords();
        Assert.assertTrue("Keywords should have 4, instead " + keywords.size(),
                          (keywords.size() == 0));
    }


    
    public void testMatching() {
        LimeXMLReplyCollection collection = 
        new LimeXMLReplyCollection(files, schemaURI, mfm, audio);

        // make sure that a simple match works....
        List nameVals = new ArrayList();
        nameVals.add(new NameValue(TITLE_KEY, "vade"));
        LimeXMLDocument searchDoc = new LimeXMLDocument(nameVals,
                                                        schemaURI);
        List results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));

        // make sure a little more complex match works, no mp3 has album but one
        // has a title beginning with s
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "susheel"));
        nameVals.add(new NameValue(TITLE_KEY, "s"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));        

        // has a title beginning with s
        nameVals.clear();
        nameVals.add(new NameValue(ALBUM_KEY, "susheel"));
        nameVals.add(new NameValue(TITLE_KEY, "s"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));        

        // has two matching, one null
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "vader"));
        nameVals.add(new NameValue(ALBUM_KEY, "darth"));
        nameVals.add(new NameValue(ARTIST_KEY, "darth"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));        

        // has two matching
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "swi"));
        nameVals.add(new NameValue(ARTIST_KEY, "ferr"));
        searchDoc = new LimeXMLDocument(nameVals, schemaURI);
        results = collection.getMatchingReplies(searchDoc);
        Assert.assertTrue("Not the correct amount of results, # = " +
                          results.size(), (results.size() == 1));        

        // has one off by one....
        nameVals.clear();
        nameVals.add(new NameValue(TITLE_KEY, "swi"));
        nameVals.add(new NameValue(ARTIST_KEY, "ferb"));
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


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite =  new TestSuite("LimeXMLReplyCollection Unit Test");
        suite.addTest(new TestSuite(CollectionTester.class));
        return suite;
    }

}
