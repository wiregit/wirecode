
package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.Response;
import com.sun.java.util.collections.*;

import java.io.*;
import java.net.*;
import junit.framework.*;

/**
 * Tests the MetaFileManager.  Subclass FileManagerTest so that
 * the same tests ran for SimpleFileManager can be run for 
 * MetaFileManager.
 */
public class MetaFileManagerTest 
    extends com.limegroup.gnutella.FileManagerTest {

    public MetaFileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MetaFileManagerTest.class);
    }

    public static void globalSetUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        try {
            RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }        
    }
    
	public void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        	    
	    cleanFiles(_sharedDir, false);
        fman = new MetaFileManager();
        PrivilegedAccessor.setValue(RouterService.class, 
                                    "fileManager",
                                    fman);
	    PrivilegedAccessor.setValue(RouterService.class, "callback", new FManCallback());
	    
	}

    public void testMetaQRT() throws Exception {
        String dir2 = "director=\"francis loopola\"";

        File f1 = createNewNamedTestFile(10, "hello");
        QueryRouteTable qrt = fman.getQRT();
        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
        waitForLoad();
        
        //make sure QRP contains the file f1
        qrt = fman.getQRT();
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));

        //now test xml metadata in the QRT
        File f2 = createNewNamedTestFile(11, "metadatafile2");
        LimeXMLDocument newDoc2 =
            new LimeXMLDocument(buildXMLString(dir2));

        List l2 = new ArrayList();
        l2.add(newDoc2);
        
        fman.addFileIfShared(f2, l2);
        qrt = fman.getQRT();
        
        assertTrue("expected in QRT", 
                   qrt.contains 
                   (get_qr(buildXMLString(dir2))));
        assertFalse("should not be in QRT", 
                    qrt.contains
                    (get_qr(buildXMLString("sasami juzo"))));
        
        //now remove the file and make sure the xml gets deleted.
        fman.removeFileIfShared(f2);
        qrt = fman.getQRT();
       
        assertFalse("should not be in QRT",
                    qrt.contains
                    (get_qr(buildXMLString(dir2))));
    }

    public void testMetaQueries() throws Exception {
        waitForLoad();
        String dir1 = "director=\"loopola\"";

        //make sure there's nothing with this xml query
        Response[] res = 
            fman.query(QueryRequest.createQuery("", 
                                                buildXMLString(dir1)));
        
        assertEquals("there should be no matches", 0, res.length);
        
        File f1 = createNewNamedTestFile(10, "test_this");
        
        LimeXMLDocument newDoc1 = 
            new LimeXMLDocument(buildXMLString(dir1));
        List l1 = new ArrayList();
        l1.add(newDoc1);


        String dir2 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f2 = createNewNamedTestFile(11, "hmm");

        LimeXMLDocument newDoc2 = 
            new LimeXMLDocument(buildXMLString(dir2));
        List l2 = new ArrayList();
        l2.add(newDoc2);

        
        String dir3 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f3 = createNewNamedTestFile(12, "testtesttest");
        
        LimeXMLDocument newDoc3 = 
            new LimeXMLDocument(buildXMLString(dir3));
        List l3 = new ArrayList();
        l3.add(newDoc3);
        
        //add files and check they are returned as responses
        fman.addFileIfShared(f1, l1);
        fman.addFileIfShared(f2, l2);
        fman.addFileIfShared(f3, l3);

        res = fman.query(QueryRequest.createQuery("", 
                                                  buildXMLString(dir1)));
        assertEquals("there should be one match", 1, res.length);

        res = fman.query(QueryRequest.createQuery("", 
                                                 buildXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);
        
        //remove a file
        fman.removeFileIfShared(f1);

        res = fman.query(QueryRequest.createQuery("", 
                                                  buildXMLString(dir1)));
        assertEquals("there should be no matches", 0, res.length);
        
        //make sure the two other files are there
        res = fman.query(QueryRequest.createQuery("",
                                                  buildXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);

        //remove another and check we still have on left
        fman.removeFileIfShared(f2);
        
        res = fman.query(QueryRequest.createQuery("",
                                                  buildXMLString(dir3)));
        
        assertEquals("there should be one match", 1, res.length);

        //remove the last file and make sure we get no replies
        fman.removeFileIfShared(f3);
        
        res = fman.query(QueryRequest.createQuery("",
                                                  buildXMLString(dir3)));
        assertEquals("there should be no matches", 0, res.length);
        
    }

    private QueryRequest get_qr(File f) {
        return 
            QueryRequest.createQuery
            (I18NConvert.instance().getNorm(f.getName()));
    }
    
    private QueryRequest get_qr(String xml) {
        return 
            QueryRequest.createQuery("", xml);
    }

    // build xml string for video
    private String buildXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video " 
            + keyname 
            + "></video></videos>";
    }


}


