package com.limegroup.gnutella.spam;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class SpamManagerTest extends BaseTestCase {

    public SpamManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SpamManagerTest.class);
    }

    /** various names of files - some tokens need to exist in each" */
    private static final String name1 = "badger badger badger";
    private static final String name2 = "badger mushroom mushroom";
    private static final String name3 = "double nil mushroom";
    
    /** addresses */
    private static final String addr1 = "1.1.1.1";
    private static final int port1 = 6346;
    private static final String addr2 = "2.2.2.2";
    private static final int port2 = 6347;
    
    /** urns */
    private static  URN urn1, urn2;
    
    /** sizes */
    private static final int size1 = 1000;
    private static final int size2 = 2000;
    
    /** xml docs */
    private static final String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
        "\"http://www.limewire.com/schemas/audio.xsd\"><audio " +
        "title=\"badger\"" +
        "></audio></audios>";
    
    private static final String xml2 = "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=" +
        "\"http://www.limewire.com/schemas/video.xsd\"><video " +
        "title=\"mushroom\"" +
        "></video></videos>";
    
    static LimeXMLDocument doc1, doc2;
    
    static SpamManager manager = SpamManager.instance();
    static RemoteFileDesc rfd1, rfd2, rfd3;
    
    public static void globalSetUp() {
        try {
            urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
            urn2 = URN.createSHA1Urn("urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
            doc1 = new LimeXMLDocument(xml1);
            doc2 = new LimeXMLDocument(xml2);
        } catch (Exception bad) {
            fail(bad);
        }
    }
    
    public void setUp() {
        manager.clearFilterData();
    }
    
    public void tearDown() {
        if (rfd1 != null)
            rfd1.setSpamRating(0f);
        if (rfd2 != null)
            rfd2.setSpamRating(0f);
        if (rfd3 != null)
            rfd3.setSpamRating(0f);
    }
    
    private static RemoteFileDesc createRFD(String addr, int port,
            String name, LimeXMLDocument doc, URN urn, int size) {
        Set urns = new HashSet();
        urns.add(urn);
        return new RemoteFileDesc(addr, port, 1, name,
                size, DataUtils.EMPTY_GUID, 3, 
                false, 3, false,
                doc, urns,
                false,false,
                "ALT",0l,
                Collections.EMPTY_SET, 0l);
    }
    
}
