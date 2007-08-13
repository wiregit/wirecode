
package com.limegroup.gnutella.xml;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.concurrent.Providers;
import org.limewire.util.I18NConvert;

import com.limegroup.gnutella.FileManagerControllerImpl;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Tests the MetaFileManager.  Subclass FileManagerTest so that
 * the same tests ran for SimpleFileManager can be run for 
 * MetaFileManager.
 */
@SuppressWarnings("unchecked")
public class MetaFileManagerTest extends com.limegroup.gnutella.FileManagerTest {

    public MetaFileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MetaFileManagerTest.class);
    }

    public static void globalSetUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        try {
            ProviderHacks.getAcceptor().setAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }        
    }
    
	public void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        	    
	    cleanFiles(_sharedDir, false);
        fman = new MetaFileManager(new FileManagerControllerImpl(Providers
                .of(ProviderHacks.getUrnCache()), Providers.of(ProviderHacks
                .getDownloadManager()), Providers.of(ProviderHacks
                .getCreationTimeCache()), Providers.of(new ContentManager()),
                Providers.of(ProviderHacks.getAltLocManager()), Providers
                        .of(ProviderHacks.getResponseFactory()), Providers
                        .of(ProviderHacks.getSavedFileManager()), Providers
                        .of(ProviderHacks.getSimppManager()), Providers
                        .of(ProviderHacks.getUpdateHandler()), Providers
                        .of(ProviderHacks.getActivityCallback()), ProviderHacks
                        .getBackgroundExecutor()));
        LimeTestUtils.setActivityCallBack(new ActivityCallbackStub());

    }
	
	public void testMetaQueriesWithConflictingMatches() throws Exception {
	    waitForLoad();
	    
	    // test a query where the filename is meaningless but XML matches.
	    File f1 = createNewNamedTestFile(10, "meaningless");
	    LimeXMLDocument d1 = new LimeXMLDocument(buildAudioXMLString(
	        "artist=\"Sammy B\" album=\"Jazz in G\""));
	    List l1 = new ArrayList(); l1.add(d1);
	    FileManagerEvent result = addIfShared(f1, l1);
	    assertTrue(result.toString(), result.isAddEvent());
	    assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
	    
	    Response[] r1 = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("sam",
	                                buildAudioXMLString("artist=\"sam\"")));
        assertNotNull(r1);
        assertEquals(1, r1.length);
        assertEquals(d1.getXMLString(), r1[0].getDocument().getXMLString());
        
        // test a match where 50% matches -- should get no matches.
        Response[] r2 = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("sam jazz in c",
                                   buildAudioXMLString("artist=\"sam\" album=\"jazz in c\"")));
        if(r2 != null)
            assertEquals(0, r2.length);
            
            
        // test where the keyword matches only.
        Response[] r3 = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("meaningles"));
        assertNotNull(r3);
        assertEquals(1, r3.length);
        assertEquals(d1.getXMLString(), r3[0].getDocument().getXMLString());
                                  
        // test where keyword matches, but xml doesn't.
        Response[] r4 = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("meaningles",
                                   buildAudioXMLString("artist=\"bob\"")));
        if(r4 != null)
            assertEquals(0, r4.length);
            
        // more ambiguous tests -- a pure keyword search for "jazz in d"
        // will work, but a keyword search that included XML will fail for
        // the same.
        File f2 = createNewNamedTestFile(10, "jazz in d");
        LimeXMLDocument d2 = new LimeXMLDocument(buildAudioXMLString(
            "album=\"jazz in e\""));
        List l2 = new ArrayList(); l2.add(d2);
        result = addIfShared(f2, l2);
	    assertTrue(result.toString(), result.isAddEvent());
	    assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        // pure keyword.
        Response[] r5 = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("jazz in d"));
        assertNotNull(r5);
        assertEquals(1, r5.length);
        assertEquals(d2.getXMLString(), r5[0].getDocument().getXMLString());
        
        // keyword, but has XML to check more efficiently.
        Response[] r6 = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("jazz in d",
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
        LimeXMLDocument newDoc2 = new LimeXMLDocument(buildVideoXMLString(dir2));
        List l2 = new ArrayList();
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
        Response[] res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("", buildVideoXMLString(dir1)));
        
        assertEquals("there should be no matches", 0, res.length);
        
        File f1 = createNewNamedTestFile(10, "test_this");
        
        LimeXMLDocument newDoc1 = new LimeXMLDocument(buildVideoXMLString(dir1));
        List l1 = new ArrayList();
        l1.add(newDoc1);


        String dir2 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f2 = createNewNamedTestFile(11, "hmm");

        LimeXMLDocument newDoc2 = new LimeXMLDocument(buildVideoXMLString(dir2));
        List l2 = new ArrayList();
        l2.add(newDoc2);

        
        String dir3 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f3 = createNewNamedTestFile(12, "testtesttest");
        
        LimeXMLDocument newDoc3 = 
            new LimeXMLDocument(buildVideoXMLString(dir3));
        List l3 = new ArrayList();
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
        res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be one match", 1, res.length);

        res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);
        
        //remove a file
        fman.removeFileIfShared(f1);

        res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be no matches", 0, res.length);
        
        //make sure the two other files are there
        res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);

        //remove another and check we still have on left
        fman.removeFileIfShared(f2);
        res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("",buildVideoXMLString(dir3)));
        assertEquals("there should be one match", 1, res.length);

        //remove the last file and make sure we get no replies
        fman.removeFileIfShared(f3);
        res = fman.query(ProviderHacks.getQueryRequestFactory().createQuery("", buildVideoXMLString(dir3)));
        assertEquals("there should be no matches", 0, res.length);
    }

    private QueryRequest get_qr(File f) {
        return 
            ProviderHacks.getQueryRequestFactory().createQuery
            (I18NConvert.instance().getNorm(f.getName()));
    }
    
    private QueryRequest get_qr(String xml) {
        return 
            ProviderHacks.getQueryRequestFactory().createQuery("", xml);
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
    
    protected FileManagerEvent addIfShared(File f, List l) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.addFileIfShared(f, l, fel);
            fel.wait(5000);
        }
        return fel.evt;
    }

}


