package com.limegroup.gnutella.xml;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.I18NConvert;

import com.google.inject.Injector;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerTest;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 * Tests the MetaFileManager.  Subclass FileManagerTest so that
 * the same tests ran for SimpleFileManager can be run for 
 * MetaFileManager.
 */
public class MetaFileManagerTest extends FileManagerTest {

    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    private QueryRequestFactory queryRequestFactory;
    
    public MetaFileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MetaFileManagerTest.class);
    }
    
    @Override
	protected void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        	
	    cleanFiles(_sharedDir, false);
	    
        Injector injector = LimeTestUtils.createInjector();

        injector.getInstance(Acceptor.class).setAddress(InetAddress.getLocalHost());

        fman = injector.getInstance(FileManager.class);

        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
	
	public void testMetaQueriesWithConflictingMatches() throws Exception {
	    waitForLoad();
	    
	    // test a query where the filename is meaningless but XML matches.
	    File f1 = createNewNamedTestFile(10, "meaningless");
	    LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(
	        "artist=\"Sammy B\" album=\"Jazz in G\""));
	    List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>(); 
	    l1.add(d1);
	    FileManagerEvent result = addIfShared(f1, l1);
	    assertTrue(result.toString(), result.isAddEvent());
	    assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
	    
	    Response[] r1 = fman.query(queryRequestFactory.createQuery("sam",
	                                buildAudioXMLString("artist=\"sam\"")));
        assertNotNull(r1);
        assertEquals(1, r1.length);
        assertEquals(d1.getXMLString(), r1[0].getDocument().getXMLString());
        
        // test a match where 50% matches -- should get no matches.
        Response[] r2 = fman.query(queryRequestFactory.createQuery("sam jazz in c",
                                   buildAudioXMLString("artist=\"sam\" album=\"jazz in c\"")));
        if(r2 != null)
            assertEquals(0, r2.length);
            
            
        // test where the keyword matches only.
        Response[] r3 = fman.query(queryRequestFactory.createQuery("meaningles"));
        assertNotNull(r3);
        assertEquals(1, r3.length);
        assertEquals(d1.getXMLString(), r3[0].getDocument().getXMLString());
                                  
        // test where keyword matches, but xml doesn't.
        Response[] r4 = fman.query(queryRequestFactory.createQuery("meaningles",
                                   buildAudioXMLString("artist=\"bob\"")));
        if(r4 != null)
            assertEquals(0, r4.length);
            
        // more ambiguous tests -- a pure keyword search for "jazz in d"
        // will work, but a keyword search that included XML will fail for
        // the same.
        File f2 = createNewNamedTestFile(10, "jazz in d");
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(
            "album=\"jazz in e\""));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>(); l2.add(d2);
        result = addIfShared(f2, l2);
	    assertTrue(result.toString(), result.isAddEvent());
	    assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        // pure keyword.
        Response[] r5 = fman.query(queryRequestFactory.createQuery("jazz in d"));
        assertNotNull(r5);
        assertEquals(1, r5.length);
        assertEquals(d2.getXMLString(), r5[0].getDocument().getXMLString());
        
        // keyword, but has XML to check more efficiently.
        Response[] r6 = fman.query(queryRequestFactory.createQuery("jazz in d",
                                   buildAudioXMLString("album=\"jazz in d\"")));
        if(r6 != null)
            assertEquals(0, r6.length);
                            
        
                                   
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
        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(newDoc2);
        
	    FileManagerEvent result = addIfShared(f2, l2);
	    assertTrue(result.toString(), result.isAddEvent());
	    assertEquals(newDoc2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        qrt = fman.getQRT();
        
        assertTrue("expected in QRT", qrt.contains (get_qr(buildVideoXMLString(dir2))));
        assertFalse("should not be in QRT", qrt.contains(get_qr(buildVideoXMLString("director=\"sasami juzo\""))));
        
        //now remove the file and make sure the xml gets deleted.
        fman.removeFileIfShared(f2);
        qrt = fman.getQRT();
       
        assertFalse("should not be in QRT", qrt.contains(get_qr(buildVideoXMLString(dir2))));
    }

    public void testMetaQueries() throws Exception {
        waitForLoad();
        String dir1 = "director=\"loopola\"";

        //make sure there's nothing with this xml query
        Response[] res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        
        assertEquals("there should be no matches", 0, res.length);
        
        File f1 = createNewNamedTestFile(10, "test_this");
        
        LimeXMLDocument newDoc1 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir1));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(newDoc1);


        String dir2 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f2 = createNewNamedTestFile(11, "hmm");

        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(newDoc2);

        
        String dir3 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f3 = createNewNamedTestFile(12, "testtesttest");
        
        LimeXMLDocument newDoc3 = 
            limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir3));
        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
        l3.add(newDoc3);
        
        //add files and check they are returned as responses
        FileManagerEvent result = addIfShared(f1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        result = addIfShared(f2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        result = addIfShared(f3, l3);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc3, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        Thread.sleep(100);
        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be one match", 1, res.length);

        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);
        
        //remove a file
        fman.removeFileIfShared(f1);

        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be no matches", 0, res.length);
        
        //make sure the two other files are there
        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);

        //remove another and check we still have on left
        fman.removeFileIfShared(f2);
        res = fman.query(queryRequestFactory.createQuery("",buildVideoXMLString(dir3)));
        assertEquals("there should be one match", 1, res.length);

        //remove the last file and make sure we get no replies
        fman.removeFileIfShared(f3);
        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir3)));
        assertEquals("there should be no matches", 0, res.length);
    }

    private QueryRequest get_qr(File f) {
        return queryRequestFactory.createQuery(I18NConvert.instance().getNorm(f.getName()));
    }
    
    private QueryRequest get_qr(String xml) {
        return queryRequestFactory.createQuery("", xml);
    }

    // build xml string for video
    private String buildVideoXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video " 
            + keyname 
            + "></video></videos>";
    }
    
    private String buildAudioXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio " 
            + keyname 
            + "></audio></audios>";
    }    
    
    protected FileManagerEvent addIfShared(File f, List<LimeXMLDocument> l) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.addFileIfShared(f, l, fel);
            fel.wait(5000);
        }
        return fel.evt;
    }

}


