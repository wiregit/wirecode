package com.limegroup.gnutella.xml;

import java.io.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

/** Unit test for LimeXMLReplyCollection.
 * Should run in the tests/xml directory (ie where this file is located).
 */
public class CollectionTester extends TestCase {

    public CollectionTester(String name) {
        super(name);
    }

    protected void setUp() {
    }

    public void testBasic() {
        Set files = new HashSet();
        File mason = new File("nullfile.null");
        if (!mason.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        File vader = new File("vader.mp3");
        if (!vader.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        File swing = new File("swing.mp3");
        if (!swing.exists())
            Assert.assertTrue("Could not find necessary file!", false);
        files.add(mason);
        files.add(vader);
        files.add(swing);
        final String schemaURI =  "http://www.limewire.com/schemas/audio.xsd";
        MetaFileManager mfm = new MFMStub();
        final boolean audio = true;
        
        // test construction
        LimeXMLReplyCollection collection = 
        new LimeXMLReplyCollection(files, schemaURI, mfm, audio);
        Assert.assertTrue("LimeXMLCollection count wrong!",
                          (collection.getCount() == 2));

        // test assocation
        LimeXMLDocument doc = null;
        doc = collection.getDocForHash(mfm.readFromMap(mason, true));
        Assert.assertTrue("Mason should not have a doc!",
                          doc == null);
        doc = collection.getDocForHash(mfm.readFromMap(vader, true));
        Assert.assertTrue("Vader should have a doc!",
                          doc != null);
        doc = collection.getDocForHash(mfm.readFromMap(swing, true));
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

    
    public void testAdvanced() {
        // current empty.
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
