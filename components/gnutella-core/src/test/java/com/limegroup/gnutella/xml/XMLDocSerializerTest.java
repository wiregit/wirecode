package com.limegroup.gnutella.xml;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;

import com.limegroup.gnutella.util.CommonUtils;

/** This class tests that present and future versions are compatible with older
 * versions of LimeXMLDocument.  Basically, for every version the current
 * version should be able to deserialize 'older' XML Docs.
 *
 * NOTE: Must be run from the directory where the relevant .sxml files reside.
 */
public class XMLDocSerializerTest extends com.limegroup.gnutella.util.BaseTestCase {

    final String fileLocation = "com/limegroup/gnutella/xml/";

    public XMLDocSerializerTest(String name) {
        super(name);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(XMLDocSerializerTest.class);
    }

    private void basicTest(File file) throws Exception  {
        assertTrue("File " + file + " cannot be found!",
                          file.exists());
        try {
            LimeXMLReplyCollection.MapSerializer ms =
            new LimeXMLReplyCollection.MapSerializer(file);
            Map map = ms.getMap();
            if (map.size() == 0)
                throw new Exception();
            Iterator keys = map.keySet().iterator();
            while (keys.hasNext()) {
                String currKey = (String) keys.next();
                LimeXMLDocument doc = (LimeXMLDocument) map.get(currKey);
            }
        }
        catch (Exception e) {
            fail("couldn't deserialize XML docs", e);
        }
    }
    
    /* Test the first version of LimeXMLDocument, versions 2.4.4 and preceding.
     */
    public void test244Pre() throws Exception {
        basicTest(CommonUtils.getResourceFile(fileLocation + "audio244Pre.sxml"));
    }

    /* This method should be changed as new versions are added.  And the current
     * version should be added as a new test.
     */
    public void testCurrent() throws Exception {
        basicTest(CommonUtils.getResourceFile(fileLocation + "audio.sxml"));
    }

}
