package com.limegroup.gnutella.uploader;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.limewire.collection.Interval;
import org.limewire.collection.IntervalSet;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.ManagedConnectionStub;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * Test that a client uploads a file correctly.  Depends on a file
 * containing the lowercase characters a-z.
 */
@SuppressWarnings("unchecked")
public class UploadTest extends LimeTestCase {
    private static final int PORT = 6668;
    /** The file name, plain and encoded. */
    private static String testDirName = "com/limegroup/gnutella/uploader/data";
    private static String incName = "partial alphabet.txt";
    private static String fileName="alphabet test file#2.txt";
    private static String encodedFile="alphabet%20test+file%232.txt";
    /** The file contents. */
	private static final String alphabet="abcdefghijklmnopqrstuvwxyz";
    /** The hash of the file contents. */
    private static final String baseHash = "GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";
    private static final String hash=   "urn:sha1:" + baseHash;
    private static final String badHash="urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";
    private static final String incompleteHash =
        "urn:sha1:INCOMCPLETEXBSQEZY37FIM5QQSA2OUJ";
    private static final int index=0;
    /** Our listening port for pushes. */
    private static final int callbackPort = 6671;
    private UploadManager upMan;
    /** The verifying file for the shared incomplete file */
    private static VerifyingFile vf;
    /** The filedesc of the shared file. */
    private FileDesc FD;    
    /** The root32 of the shared file. */
    private String ROOT32;
    /**
     * Features for push loc testing.
     */
	private static String FALTFeatures, FWALTFeatures;    

    private static RouterService ROUTER_SERVICE;
    
    private static TestUploadManager UPLOAD_MANAGER;
    
    private static final Object loaded = new Object();

	/**
	 * Creates a new UploadTest with the specified name.
	 */
	public UploadTest(String name) {
		super(name);
	}

	/**
	 * Allows this test to be run as a set of suites.
	 */
	public static Test suite() {
		return buildTestSuite(UploadTest.class);
	}

	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}
	
	/**
	 * Simple copy.  Horrible performance for large files.
	 * Good performance for alphabets.
	 */
	private static void copyFile(File source, File dest) throws Exception {
	    FileInputStream fis = new FileInputStream(source);
	    FileOutputStream fos = new FileOutputStream(dest);
	    int read = fis.read();
	    while(read != -1) {
	        fos.write(read);
	        read = fis.read();
	    }
	    fis.close();
	    fos.close();
    }

    public static void globalSetUp() {
        vf = new VerifyingFile(252450);
        
        ROUTER_SERVICE = new RouterService(new FManCallback());
        UPLOAD_MANAGER = new TestUploadManager();
        
        // Overwrite the original UploadManager with 
        // our custom TestUploadManager. See latter
        // for more Info!
        try {
            PrivilegedAccessor.setValue(ROUTER_SERVICE, "uploadManager", UPLOAD_MANAGER);
        } catch (Exception e) {
            fail(e);
        }
    }
    
	protected void setUp() throws Exception {
	    SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
		FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
		    new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*",InetAddress.getLocalHost().getHostAddress()});
        RouterService.getIpFilter().refreshHosts();
        ConnectionSettings.PORT.setValue(PORT);

        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt");
        UploadSettings.HARD_MAX_UPLOADS.setValue(10);
		UploadSettings.UPLOADS_PER_PERSON.setValue(10);
		UploadSettings.MAX_PUSHES_PER_HOST.setValue(9999);

        FilterSettings.FILTER_DUPLICATES.setValue(false);

		ConnectionSettings.NUM_CONNECTIONS.setValue(8);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        
		File testDir = CommonUtils.getResourceFile(testDirName);
		assertTrue("test directory could not be found", testDir.isDirectory());
		File testFile = new File(testDir, fileName);
		assertTrue("test file should exist", testFile.exists());
		File sharedFile = new File(_sharedDir, fileName);
		// we must use a seperate copy method
		// because the filename has a # in it which can't be a resource.
		copyFile(testFile, sharedFile);
		assertTrue("should exist", new File(_sharedDir, fileName).exists());
		assertGreaterThan("should have data", 0, new File(_sharedDir, fileName).length());

        if ( !RouterService.isLoaded() ) {
            startAndWaitForLoad();
            Thread.sleep(2000);
        }
	    
        assertEquals("ports should be equal",
                     PORT, ConnectionSettings.PORT.getValue());
                     
        upMan = RouterService.getUploadManager();
        
        // Make sure our customized UploadManager is set in
        // RouterService and clear its activeUploads
        // cache. See TestUploadManager for more Info!
        assertTrue(upMan == UPLOAD_MANAGER);
        UPLOAD_MANAGER.clearUploads();
        
        FileManager fm = RouterService.getFileManager();
        File incFile = new File(_incompleteDir, incName);
        CommonUtils.copyResourceFile(testDirName + "/" + incName, incFile);
        URN urn = URN.createSHA1Urn(incompleteHash);
        Set urns = new HashSet();
        urns.add(urn);
        fm.addIncompleteFile(incFile, urns, incName, 1981, vf);
        assertEquals( 1, fm.getNumIncompleteFiles() );
        assertEquals( 1, fm.getNumFiles() );
        FD = fm.getFileDescForFile(new File(_sharedDir, fileName));
        while(FD.getHashTree() == null)
            Thread.sleep(300);
        ROOT32 = FD.getHashTree().getRootHash();
        
        // remove all alts for clarity.
        RouterService.getAltlocManager().purge();

        try {Thread.sleep(300); } catch (InterruptedException e) { }
		
		assertEquals("unexpected uploads in progress",
		    0, upMan.uploadsInProgress() );
        assertEquals("unexpected queued uploads",
            0, upMan.getNumQueuedUploads() );
        
        // clear the cache history so no banning occurs.
        Map requests = (Map)PrivilegedAccessor.getValue(upMan, "REQUESTS");
        requests.clear();
        
		FALTFeatures= HTTPHeaderName.FEATURES+
			": "+ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE.httpStringValue();
		FWALTFeatures= HTTPHeaderName.FEATURES+
			": "+ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE.httpStringValue();        
	}

    //public void testAll() {
        //UploadTest works fine in isolation, but this sleep seems to be
        //needed to work as part of AllTests.  I'm not sure why.
        //try {Thread.sleep(200); } catch (InterruptedException e) { }
            
    //} 

    ///////////////////push downloads with HTTP1.0///////////
    public void testHTTP10Push() throws Exception {
        boolean passed = false;
        passed = downloadPush(fileName, null,alphabet);
        assertTrue("Push download",passed);
    }

    public void testHTTP10PushEncodedFile() throws Exception {
        boolean passed = false;        
        passed=downloadPush(encodedFile, null,alphabet);
        assertTrue("Push download, encoded file name",passed);
    }

    public void testHTTP10PushRange() throws Exception {
        boolean passed = false;
        passed =downloadPush(fileName, "Range: bytes=2-5","cdef");
        assertTrue("Push download, middle range, inclusive",passed);
    }

    ///////////////////push downloads with HTTP1.1///////////////            
    public void testHTTP11Push() throws Exception {
        boolean passed = false;
        passed = downloadPush1(fileName, null, alphabet);
        assertTrue("Push download with HTTP1.1",passed);
    }
    
    /**
     * tests the scenario where we receive the same push request message
     * more than once
     */
    public void testDuplicatePushes() throws Exception {
        
        Connection c = createConnection();
        c.initialize(new UltrapeerHeaders(null), new EmptyResponder(), 1000);
        QueryRequest query=QueryRequest.createQuery("txt", (byte)3);
        c.send(query);
        c.flush();
        QueryReply reply=null;
        for(int i = 0; i < 10; i++) {
            Message m=c.receive(2000);
            if (m instanceof QueryReply) {
                reply=(QueryReply)m;
                break;
            } 
        }
        
        if(reply == null)
            throw new IOException("didn't get query reply in time");
        
        PushRequest push =
            new PushRequest(GUID.makeGuid(),
                    (byte)3,
                    reply.getClientGUID(),
                    0,
                    new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
                    callbackPort);
        
        //Create listening socket, then send the push a few times
        ServerSocket ss=new ServerSocket(callbackPort);
        c.send(push);
        c.send(push); 
        c.send(push);
        c.flush();
        ss.setSoTimeout(2000);
        
        assertNotNull(ss.accept()); // get one.
        
        // the last two shouldn't be gotten.
        try {
            ss.accept();
            fail("node replied to duplicate push request");
        }catch(IOException expected){}
        
        try {
            ss.accept();
            fail("node replied to duplicate push request");
        }catch(IOException expected){}
        
        ss.close();
        c.close();
        
    }
     

    public void testHTTP11PushEncodedFile() throws Exception {
        boolean passed = false;
        passed =downloadPush1(encodedFile, null,
                         "abcdefghijklmnopqrstuvwxyz");
        assertTrue("Push download, encoded file name with HTTP1.1",passed);
    }

    public void testHTTP11PushRange() throws Exception {
        boolean passed = false;
        passed =downloadPush1(fileName, "Range: bytes=2-5","cdef");
        assertTrue("Push download, middle range, inclusive with HTTP1.1",passed);
    }
     

    public void testHTTP11Head() throws Exception {
        assertTrue("Persistent push HEAD requests", 
                   downloadPush1("HEAD", "/get/"+index+"/"+encodedFile, null, ""));
    }
        
                       

    //////////////normal downloads with HTTP 1.0//////////////

    public void testHTTP10Download() throws Exception {
        boolean passed = false;
        passed =download(fileName, null,"abcdefghijklmnopqrstuvwxyz");
        assertTrue("No range header",passed);
    }
    
    public void testHTTP10DownloadRange() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range: bytes=2-", 
                    "cdefghijklmnopqrstuvwxyz");
        assertTrue("Standard range header",passed);
    }

    public void testHTTP10DownloadMissingRange() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range: bytes 2-", 
                    "cdefghijklmnopqrstuvwxyz");
        assertTrue("Range missing \"=\".  (Not legal HTTP, but common.)",
               passed);
    }

    public void testHTTP10DownloadMiddleRange() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range: bytes=2-5","cdef",
                    "Content-range: bytes 2-5/26");
        assertTrue("Middle range, inclusive",passed);
    }

    // FIXME is that seriously a valid header?
//    public void testHTTP10DownloadRangeNoSpace() throws Exception {
//        boolean passed = false;
//        passed =download(fileName, "Range:bytes 2-",
//                    "cdefghijklmnopqrstuvwxyz",
//                    "Content-length: 24");
//        assertTrue("No space after \":\".  (Legal HTTP.)",passed);
//    }

    public void testHTTP10DownloadRangeLastByte() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range: bytes=-5","vwxyz");
        assertTrue("Last bytes of file",passed);
    }

    public void testHTTP10DownloadRangeTooBigNegative() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range: bytes=-30",
                    "abcdefghijklmnopqrstuvwxyz");
        assertTrue("Too big negative range request",passed);
    }


    public void testHTTP10DownloadRangeExtraSpace() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range:   bytes=  2  -  5 ", "cdef");
        assertTrue("Lots of extra space",passed);
    }


    public void testHTTP10DownloadURLEncoding() throws Exception {
        assertEquals("Unexpected: "+ URLDecoder.decode(encodedFile), fileName,
                     URLDecoder.decode(encodedFile));
        boolean passed = false;
        passed =download(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
        assertTrue("URL encoded",passed);
    }

    ////////////normal download with HTTP 1.1////////////////
    public void testHTTP11DownloadNoRangeHeader() throws Exception {
        boolean passed = false;
        passed =download1(fileName, null,"abcdefghijklmnopqrstuvwxyz", false);
        assertTrue("No range header with HTTP1.1",passed);
    }

    public void testHTTP11DownloadStandardRangeHeader() throws Exception {
        boolean passed = false;
        passed =download1(fileName, "Range: bytes=2-", 
                     "cdefghijklmnopqrstuvwxyz", false);
        assertTrue("Standard range header with HTTP1.1",passed);
    }    


    public void testHTTP11DownloadRangeMissingEquals() throws Exception {
        boolean passed = false;
        passed =download1(fileName, "Range: bytes 2-", 
                     "cdefghijklmnopqrstuvwxyz", false);
        assertTrue("Range missing \"=\". (Not legal HTTP, but common.)"+
               "with HTTP1.1", passed);
    }

    public void testHTTP11DownloadMiddleRange() throws Exception {
        boolean passed = false;
        passed =download1(fileName, "Range: bytes=2-5","cdef", false);
        assertTrue("Middle range, inclusive with HTTP1.1",passed);
    }
        
    public void testHTTP11DownloadRangeNoSpaceAfterColon() throws Exception {
        boolean passed = false;
        passed =download1(fileName, "Range:bytes 2-",
                     "cdefghijklmnopqrstuvwxyz", false);
        assertTrue("No space after \":\".  (Legal HTTP.) with HTTP1.1",passed);
    }

    public void testHTTP11DownloadRangeLastByte() throws Exception {
        boolean passed = false;
        passed =download1(fileName, "Range: bytes=-5","vwxyz", false);
        assertTrue("Last bytes of file with HTTP1.1",passed);
    }


    public void testHTTP11DownloadRangeLotsOfExtraSpace() throws Exception {
        boolean passed = false;
        passed =download1(fileName, "Range:   bytes=  2  -  5 ", "cdef", false);
        assertTrue("Lots of extra space with HTTP1.1",passed);        
    }

    public void testHTTP11IncompleteRange() throws Exception {
        boolean passed = false;
        // add the range.
        Interval iv = new Interval(2, 6);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,"verifiedBlocks");
        vb.add(iv);
        passed = download1(incompleteHash, "Range: bytes 2-5", "cdef", true);
        assertTrue("incomplete range did not work", passed);
        
        passed = download1(incompleteHash, "Range: bytes 1-3", "cd", true);
        assertTrue("didn't shrink wanted ranges, low", passed);
        
        passed = download1(incompleteHash, "Range: bytes 3-10", "defg", true);
        assertTrue("didn't shrink wanted ranges, high", passed);
        
        passed = download1(incompleteHash, "Range: bytes 0-20", "cdefg", true);
        assertTrue("didn't shrink wanted ranges, both", passed);
        
        // failures checked in testIncompleteXXX later on down.
    }
    
    public void testHTTP11DownloadURLEncoding() throws Exception {
        boolean passed = false;
        
        assertEquals("URLDecoder broken",
            fileName, URLDecoder.decode(encodedFile));

        passed =download1(encodedFile, null,"abcdefghijklmnopqrstuvwxyz", false);
        assertTrue("URL encoded with HTTP1.1",passed);

    }

//////////////////Pipelining tests with HTTP1.1//////////////             
    public void testHTTP11PipeliningDownload() throws Exception {
        boolean passed = false;
        passed = pipelineDownloadNormal(fileName, null, 
                                    "abcdefghijklmnopqrstuvwxyz");
        assertTrue("piplining with normal download",passed);
    }
            
    public void testHTTP11PipeliningDownloadPush() throws Exception {
        boolean passed = false;
        passed = pipelineDownloadPush(fileName,null, 
                                       "abcdefghijklmnopqrstuvwxyz");
        assertTrue("piplining with push download",passed);
    }
         
    public void testHTTP11DownloadMixedPersistent() throws Exception {
        tMixedPersistentRequests();
    }

    public void testHTTP11DownloadPersistentURI() throws Exception {
        tPersistentURIRequests();
    }
    
    public void testFALTNotRequested() throws Exception {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		URN sha1 = URN.createSHA1Urn(hash);
		GUID clientGUID = new GUID(GUID.makeGuid());
		GUID clientGUID2 = new GUID(GUID.makeGuid());
		
		AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",sha1);
		AlternateLocation push = AlternateLocation.create(
				clientGUID.toHexString()+";1.2.3.4:5",sha1);
        ((PushAltLoc)push).updateProxies(true);
		PushAltLoc pushFwt = (PushAltLoc) 
			AlternateLocation.create(
		        clientGUID2.toHexString()+";fwt/1.0;1.2.3.4:6",sha1);
        pushFwt.updateProxies(true);
		
		
		RouterService.getAltlocManager().add(direct, null);
        RouterService.getAltlocManager().add(push, null);
        RouterService.getAltlocManager().add(pushFwt, null);
		
		assertEquals(0,((PushAltLoc)push).supportsFWTVersion());
		assertEquals(1,pushFwt.supportsFWTVersion());
		assertEquals(3,RouterService.getAltlocManager().getNumLocs(FD.getSHA1Urn()));        
        
        boolean passed = download(fileName, 
                                  "X-Alt:",
                                  "abcdefghijklmnopqrstuvwxyz",
            new Applyable() {
                public void apply(String line) throws Exception {
                    if(line.toLowerCase().startsWith("x-falt:"))
                        fail("had line: " + line);
                    else if(!line.startsWith("HTTP")) // check only the first time.
                        return;
                    
                    UploadManager umanager = RouterService.getUploadManager();
		            List l;
		            HTTPUploader u;
		            synchronized(umanager){
                        // See TestUploadManager for more info!
		                l = UPLOAD_MANAGER.activeUploads;
		                assertEquals(1,l.size());
		                u = (HTTPUploader)l.get(0);
		            }
		            assertFalse(u.getAltLocTracker().wantsFAlts());
		            assertEquals(0,u.getAltLocTracker().getFwtVersion());
                }
            }
        );
        assertTrue(passed);
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    }
    
    public void testFALTWhenRequested() throws Exception {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		URN sha1 = URN.createSHA1Urn(hash);
		GUID clientGUID = new GUID(GUID.makeGuid());
		GUID clientGUID2 = new GUID(GUID.makeGuid());
		
		AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",sha1);
		final AlternateLocation push = AlternateLocation.create(
				clientGUID.toHexString()+";1.2.3.4:5",sha1);
        ((PushAltLoc)push).updateProxies(true);
		final PushAltLoc pushFwt = (PushAltLoc) 
			AlternateLocation.create(
		        clientGUID2.toHexString()+";fwt/1.0;1.2.3.4:6",sha1);
        pushFwt.updateProxies(true);
		
		
        RouterService.getAltlocManager().add(direct, null);
        RouterService.getAltlocManager().add(push, null);
        RouterService.getAltlocManager().add(pushFwt, null);
		
		
		assertEquals(0,((PushAltLoc)push).supportsFWTVersion());
		assertEquals(1,pushFwt.supportsFWTVersion());
		assertEquals(3,RouterService.getAltlocManager().getNumLocs(FD.getSHA1Urn()));                
        
        boolean passed = download(fileName,
                                  FALTFeatures,
                                  "abcdefghijklmnopqrstuvwxyz",
                                  "X-FAlt: " + push.httpStringValue() + ", " + pushFwt.httpStringValue(),
            new Applyable() {
                public void apply(String line) throws Exception {
                    if(!line.startsWith("HTTP")) // check only the first time
                        return;
	                UploadManager umanager = RouterService.getUploadManager();
	                List l;
	                HTTPUploader u;
	                synchronized(umanager){
                        // See TestUploadManager for more info!
                        l = UPLOAD_MANAGER.activeUploads;
                        assertEquals(1,l.size());
	                    u = (HTTPUploader)l.get(0);
	                }
		            assertTrue(u.getAltLocTracker().wantsFAlts());
 		            assertEquals(0,u.getAltLocTracker().getFwtVersion());        
                }
            }
       );
       assertTrue(passed);
       Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    }
    
    public void testFWALTWhenRequested() throws Exception {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		URN sha1 = URN.createSHA1Urn(hash);
		GUID clientGUID = new GUID(GUID.makeGuid());
		GUID clientGUID2 = new GUID(GUID.makeGuid());
		
		AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",sha1);
		final AlternateLocation push = AlternateLocation.create(
				clientGUID.toHexString()+";1.2.3.4:5",sha1);
        ((PushAltLoc)push).updateProxies(true);
		final PushAltLoc pushFwt = (PushAltLoc) 
			AlternateLocation.create(
		        clientGUID2.toHexString()+";fwt/1.0;1.2.3.4:6",sha1);
        pushFwt.updateProxies(true);
		
		
        RouterService.getAltlocManager().add(direct, null);
        RouterService.getAltlocManager().add(push, null);
        RouterService.getAltlocManager().add(pushFwt, null);
		
		assertEquals(0,((PushAltLoc)push).supportsFWTVersion());
		assertEquals(1,pushFwt.supportsFWTVersion());
		assertEquals(3,RouterService.getAltlocManager().getNumLocs(FD.getSHA1Urn()));             
        
        boolean passed = download(fileName,
                                  FWALTFeatures,
                                  "abcdefghijklmnopqrstuvwxyz",
                                  "X-FAlt: " + pushFwt.httpStringValue(),
            new Applyable() {
                public void apply(String line) throws Exception {
                    if(!line.startsWith("HTTP")) // check only the first time.
                        return;
                    UploadManager umanager = RouterService.getUploadManager();
                    List l;
                    HTTPUploader u;
                    synchronized(umanager) {
                        // See TestUploadManager for more info!
                        l = UPLOAD_MANAGER.activeUploads;
                        assertEquals(1,l.size());
                        u = (HTTPUploader)l.get(0);
                    }
                    assertTrue(u.getAltLocTracker().wantsFAlts());
                    assertEquals((int)HTTPConstants.FWT_TRANSFER_VERSION,u.getAltLocTracker().getFwtVersion());
                }
            }
       );
       assertTrue(passed);
       Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
    }
    
    public void testUploaderStoresAllAlts() throws Exception {
        URN sha1 = URN.createSHA1Urn(hash);
		GUID clientGUID = new GUID(GUID.makeGuid());
		
		AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",sha1);
		AlternateLocation push = AlternateLocation.create(
				clientGUID.toHexString()+";1.2.3.4:5",sha1);
        ((PushAltLoc)push).updateProxies(true);
        assertEquals(0, RouterService.getAltlocManager().getNumLocs(FD.getSHA1Urn()));
        boolean passed = download(fileName,
                          "X-Alt: " + direct.httpStringValue() + "\r\n" + 
                          "X-FAlt: " + push.httpStringValue(),
                          "abcdefghijklmnopqrstuvwxyz"
                         );
        assertTrue(passed);
		assertEquals(2,RouterService.getAltlocManager().getNumLocs(FD.getSHA1Urn()));
		assertEquals(1,RouterService.getAltlocManager().getPush(FD.getSHA1Urn(),false).getAltLocsSize());
		assertEquals(1,RouterService.getAltlocManager().getDirect(FD.getSHA1Urn()).getAltLocsSize());
		
		assertTrue(RouterService.getAltlocManager().getPush(FD.getSHA1Urn(),false).contains(push));
		assertTrue(RouterService.getAltlocManager().getDirect(FD.getSHA1Urn()).contains(direct));
    }
    
    // Tests for alternate locations
    public void testAlternateLocationAddAndRemove() throws Exception {
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        // Ensure that one removal doesn't stop it.
        RouterService.getAltlocManager().remove(al, null);
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        //Add a second one, so we can check to make sure
        //another removal removes the first one.
        String loc2 = "http://2.2.2.2:2/uri-res/N2R?" + hash;
        AlternateLocation al2 = AlternateLocation.create(loc2);
        RouterService.getAltlocManager().add(al2, null);
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 2.2.2.2:2, 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        //Remove the first guy again, should only have loc2 left.
        RouterService.getAltlocManager().remove(al, null);
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 2.2.2.2:2");
        assertTrue("alt failed", passed);
    }
    
    //Tests that headers the downloader gives are used.
    public void testSentHeaderIsUsed() throws Exception {
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        // Add a header that gives a new location.
        String sendLoc = "http://2.2.2.2:2/uri-res/N2R?" + hash;
        AlternateLocation sendAl = AlternateLocation.create(sendLoc);
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Make sure the FD has that loc now.
        AlternateLocationCollection alc = RouterService.getAltlocManager().getDirect(FD.getSHA1Urn());
        assertEquals("wrong # locs", 2, alc.getAltLocsSize());
        List alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );
        
        //Make sure a request will give us both locs now.
        passed = download("/uri-res/N2R?" + hash,
                          null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 2.2.2.2:2, 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        //Demote the location (don't remove)
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Should still have it.
        assertEquals("wrong # locs", 2, alc.getAltLocsSize());
        alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );
        
        //Now remove.
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Now is removed.
        assertEquals("wrong # locs", 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }
    
    //Tests that headers the downloader gives are used.
    public void testMiniNewHeaderIsUsed() throws Exception {
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        // Add a header that gives a new location.
        String sendLoc = "http://2.2.2.2:2/uri-res/N2R?" + hash;
        AlternateLocation sendAl = AlternateLocation.create(sendLoc);
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: 2.2.2.2:2",
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Make sure the FD has that loc now.
        AlternateLocationCollection alc = RouterService.getAltlocManager().getDirect(FD.getSHA1Urn());
        assertEquals("wrong # locs", 2, alc.getAltLocsSize());
        List alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );
        
        //Make sure a request will give us both locs now.
        passed = download("/uri-res/N2R?" + hash,
                          null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 2.2.2.2:2, 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        //Demote the location (don't remove)
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: 2.2.2.2:2",
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Should still have it.
        assertEquals("wrong # locs", 2, alc.getAltLocsSize());
        alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );
        
        //Now remove (try interchanging with old header)
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Now is removed.
        assertEquals("wrong # locs", 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
        
        //Now try a header without a port, should be 6346.
        sendLoc = "http://2.3.4.5:6346/uri-res/N2R?" + hash;
        sendAl = AlternateLocation.create(sendLoc);
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: 2.3.4.5",
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Make sure the FD has that loc now.
        assertEquals("wrong # locs", 2, alc.getAltLocsSize());
        alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );        
    }
    
    // Tests that headers with multiple values in them are
    // read correctly
    public void testMultipleAlternates() throws Exception {
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        // Add a header that gives a new location.
        String send1 = "http://1.2.3.1:1/uri-res/N2R?" + hash;
        AlternateLocation al1 = AlternateLocation.create(send1);
        String send2 = "http://1.2.3.2:2/uri-res/N2R?" + hash;
        AlternateLocation al2 = AlternateLocation.create(send2);
        String send3 = "http://1.2.3.4:6346/uri-res/N2R?" + hash;
        AlternateLocation al3 = AlternateLocation.create(send3);
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + send1 + ", " + send2 + ", 1.2.3.4",
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // Make sure the FD has that loc now.
        AlternateLocationCollection alc = RouterService.getAltlocManager().getDirect(FD.getSHA1Urn());
        assertEquals("wrong # locs", 4, alc.getAltLocsSize());
        List alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(al1) );
        assertTrue( alts.contains(al2) );
        assertTrue( alts.contains(al3) );
        
        //Demote.
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: 1.2.3.1:1, " + send2 + ", " + send3,
                          "abcdefghijklmnopqrstuvwxyz");
        assertTrue("alt failed", passed);
        // Should still have it.
        assertEquals("wrong # locs", 4, alc.getAltLocsSize());
        
        //Remove
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: " + send1 + ", 1.2.3.2:2, " + send3,
                          "abcdefghijklmnopqrstuvwxyz");
        assertTrue("alt failed", passed);
        // Now is removed.
        assertEquals("wrong # locs", 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }        
    
    /**
     * tests that when reading the NFAlt header we only remove proxies
     */
    public void testRemovingNFAlt() throws Exception {
        
        boolean passed;
        GUID g = new GUID(GUID.makeGuid());
        
        
        URN urn = URN.createSHA1Urn(hash);
        
        PushAltLoc abc = (PushAltLoc)AlternateLocation.create(
                g.toHexString()+";1.1.1.1:1;2.2.2.2:2;3.3.3.3:3",
                urn);
        String abcHttp = abc.httpStringValue();
        
        PushAltLoc bcd=(PushAltLoc)AlternateLocation.create(
                g.toHexString()+";2.2.2.2:2;3.3.3.3:3;4.4.4.4:4",
                urn);
        bcd.updateProxies(true);
        
        String bcdHttp = bcd.httpStringValue();
        
        RouterService.getAltlocManager().add(bcd, null);
        assertEquals(1,RouterService.getAltlocManager().getNumLocs(urn));
        
        
        passed=download("/uri-res/N2R?" + hash,
                "X-NFAlt: " + abcHttp,
                "abcdefghijklmnopqrstuvwxyz");
        assertTrue("alt failed", passed);
        
        //two of the proxies of bcd should be gone
        assertEquals("wrong # locs", 1, RouterService.getAltlocManager().getPush(FD.getSHA1Urn(),false).getAltLocsSize());
        assertEquals("wrong # proxies",1,bcd.getPushAddress().getProxies().size());
        
        
        //now repeat, sending all three original proxies of bce as NFAlts
        Thread.sleep(1000);
        passed=download("/uri-res/N2R?" + hash,
                "X-NFAlt: " + bcdHttp,
                "abcdefghijklmnopqrstuvwxyz");
        assertTrue("alt failed", passed);
        
        // all proxies should be gone, and bcd should be removed from 
        // the filedesc
        assertEquals("wrong # locs", 0, RouterService.getAltlocManager().getPush(FD.getSHA1Urn(),false).getAltLocsSize());
        assertEquals("wrong # proxies",0,bcd.getPushAddress().getProxies().size());
    }
    
    // unfortunately we can't test with private addresses
    // because all these connections require that local_is_private
    // is false, which turns off isPrivateAddress checking.
    public void testInvalidAltsAreIgnored() throws Exception {
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        // Add an invalid alt
        String invalidAddr = "http://0.0.0.0:6346/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidAddr,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // FD should still only have 1
        AlternateLocationCollection alc = RouterService.getAltlocManager().getDirect(FD.getSHA1Urn());
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
        
        invalidAddr = "http://255.255.255.255:6346/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidAddr,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());

        // Add an invalid port
        String invalidPort = "http://1.2.3.4:0/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidPort,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
        
        invalidPort = "http://1.2.3.4:-2/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidPort,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }
    
    public void test10AltsAreSent() throws Exception {
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        for(int i = 0; i < 20; i++) {
            RouterService.getAltlocManager().add( AlternateLocation.create(
                "http://1.1.1." + i + ":6346/uri-res/N2R?" + hash), null);
        }
        assertEquals(21, RouterService.getAltlocManager().getDirect(FD.getSHA1Urn()).getAltLocsSize());
    
        String pre = "1.1.1.";
        String post = "";
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = "X-Alt: " + 
                           pre + 16 + post + comma +
                           pre + 13 + post + comma +
                           pre + 10 + post + comma +    
                           pre + 15 + post + comma +
                           pre + 12 + post + comma +
                           pre + 1 + post + comma +
                           pre + 14 + post + comma +
                           pre + 11 + post + comma +
                           pre + 0 + post + comma +
                           pre + "1:1";
        
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          required);
        assertTrue(passed);
    }
    
    public void testAltsExpire() throws Exception {
        UploadSettings.LEGACY_EXPIRATION_DAMPER.setValue((float)Math.E - 0.2f);
        // test that an altloc will expire if given out too often
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
        RouterService.getAltlocManager().add(al, null);
        
        // send it out several times
        int i = 0;
        try {
            for (i = 0; i < 10; i++) {
                download("/uri-res/N2R?" + hash, null,
                        "abcdefghijklmnopqrstuvwxyz",
                        "X-Alt: 1.1.1.1:1");
            }
            fail("altloc didn't expire");
        } catch (AssertionFailedError expected) {}
        assertLessThan(10, i);
        assertFalse(al.canBeSent(AlternateLocation.MESH_LEGACY));
        
        // now add the altloc again, it will be reset
        RouterService.getAltlocManager().add(al, null);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
    }
    
    public void testAltsDontExpire() throws Exception {
        UploadSettings.LEGACY_EXPIRATION_DAMPER.setValue((float)Math.E/4);
        // test that an altloc will not expire if given out less often
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
        RouterService.getAltlocManager().add(al, null);
        
        for (int i = 0; i < 10; i++) {
            assertTrue(download("/uri-res/N2R?" + hash, null,
                    "abcdefghijklmnopqrstuvwxyz",
            "X-Alt: 1.1.1.1:1"));
            Thread.sleep(8*1000);
        }
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
    }
    
    /**
     * tests that when an altloc has expired from all the meshes it is removed.
     */
    public void testExpiredAltsRemoved() throws Exception {
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[]{"*.*.*.*"});
        RouterService.getIpFilter().refreshHosts();
        // set the expiration values to the bare minimum 
        UploadSettings.LEGACY_BIAS.setValue(0f);
        UploadSettings.PING_BIAS.setValue(0f);
        UploadSettings.RESPONSE_BIAS.setValue(0f);
        
        // create an altloc
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
        assertTrue(al.canBeSent(AlternateLocation.MESH_PING));
        assertTrue(al.canBeSent(AlternateLocation.MESH_RESPONSE));
        RouterService.getAltlocManager().add(al, null);
        
        // drain the meshes in various orders
        drainLegacy();
        drainPing();
        drainResponse();
        assertFalse(RouterService.getAltlocManager().hasAltlocs(al.getSHA1Urn()));
        
        // and re-add the altloc
        al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        
        // repeat
        drainResponse();
        drainLegacy();
        drainPing();
        assertFalse(RouterService.getAltlocManager().hasAltlocs(al.getSHA1Urn()));
        
        al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        
        // repeat 2
        drainPing();
        drainResponse();
        drainLegacy();
        assertFalse(RouterService.getAltlocManager().hasAltlocs(al.getSHA1Urn()));
        
        UploadSettings.LEGACY_BIAS.revertToDefault();
        UploadSettings.PING_BIAS.revertToDefault();
        UploadSettings.RESPONSE_BIAS.revertToDefault();
    }
    
    private void drainLegacy() throws Exception {
        int i = 0;
        try {
            for (; i < 20; i++) {
                download("/uri-res/N2R?" + hash, null,
                        "abcdefghijklmnopqrstuvwxyz",
                        "X-Alt: 1.1.1.1:1");
            }
            fail("altloc didn't expire");
        } catch (AssertionFailedError expected) {}
        assertGreaterThan(1,i);
        assertLessThan(20,i);
    }
    
    private void drainPing() throws Exception {
        HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),FD.getSHA1Urn(),HeadPing.ALT_LOCS);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ping.write(baos);
        byte [] data = baos.toByteArray();
        DatagramPacket toSend = new DatagramPacket(data,data.length, 
                new InetSocketAddress(InetAddress.getLocalHost(),PORT));
        
        int i = 0;
        for (; i < 20; i++) {
            DatagramSocket sock = null;
            try {
                sock = new DatagramSocket(10000+i);
                sock.setSoTimeout(2000);
                sock.send(toSend);
                byte [] recv = new byte[1000];
                DatagramPacket rcv = new DatagramPacket(recv,recv.length);
                sock.receive(rcv);
                ByteArrayInputStream bais = new ByteArrayInputStream(recv,0,rcv.getLength());
                HeadPong pong = (HeadPong) MessageFactory.read(bais);
                if (pong.getAltLocs().isEmpty())
                    break;
            } finally {
                if (sock != null)
                    sock.close();
            }
        }
        
        assertGreaterThan(1,i);
        assertLessThan(20,i);
    }
    
    private void drainResponse() throws Exception {
        FilterSettings.FILTER_HASH_QUERIES.setValue(false); // easier with hash
        MyReplyHandler handler = new MyReplyHandler();
        
        assertTrue(RouterService.getAltlocManager().hasAltlocs(FD.getSHA1Urn()));
        int i = 0;
        for (; i < 20; i++) {
            QueryRequest request = QueryRequest.createQuery(FD.getSHA1Urn());
            RouterService.getMessageRouter().handleMessage(request, handler);
            assertNotNull(handler.received);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            handler.received.write(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            QueryReply reply = (QueryReply) MessageFactory.read(bais);
            Response resp = reply.getResultsArray()[0];
            if (resp.getLocations().isEmpty())
                break;
            handler.received = null;
        }
        
        assertGreaterThan(1,i);
        assertLessThan(20,i);
        FilterSettings.FILTER_HASH_QUERIES.revertToDefault();
    }
    
    private static class MyReplyHandler extends ManagedConnectionStub {
        public QueryReply received;
        public void handleQueryReply(QueryReply queryReply, ReplyHandler receivingConnection) {
            received = queryReply;
        }
        
    }
    public void testChunksGiveDifferentLocs() throws Exception {

        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        
        for(int i = 0; i < 20; i++) {
            RouterService.getAltlocManager().add( AlternateLocation.create(
                "http://1.1.1." + i + ":6346/uri-res/N2R?" + hash), null);
        }
        assertEquals(21, RouterService.getAltlocManager().getNumLocs(FD.getSHA1Urn()));        
        
        //1. Write request
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
            
        String reqFile = "/uri-res/N2R?" + hash;
        String pre = "1.1.1.";
        String post = "";
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = null;

        required = "X-Alt: " + 
                   pre + 16 + post + comma +
                   pre + 13 + post + comma +
                   pre + 10 + post + comma +    
                   pre + 15 + post + comma +
                   pre + 12 + post + comma +
                   pre + 1 + post + comma +
                   pre + 14 + post + comma +
                   pre + 11 + post + comma +
                   pre + 0 + post + comma +
                   pre + "1:1";
        downloadInternal11(reqFile,
                         "Range: bytes=0-1",
                         out, in,
                         required);
                         
        required = "X-Alt: " + 
                   pre + 5 + post + comma +
                   pre + 2 + post + comma +
                   pre + 18 + post + comma +
                   pre + 7 + post + comma +
                   pre + 4 + post + comma +
                   pre + 17 + post + comma +
                   pre + 6 + post + comma +
                   pre + 3 + post + comma +
                   pre + 19 + post + comma +
                   pre + 8 + post;
        downloadInternal11(reqFile,
                        "Range: bytes=2-3",
                        out, in,
                        required);

        required = "X-Alt: " + pre + 9 + post;
        downloadInternal11(reqFile,
                         "Range: bytes=4-5",
                         out, in,
                         required);
                         
        // Now if some more are added to file desc, make sure they're reported.
        RouterService.getAltlocManager().add( AlternateLocation.create(
                "http://1.1.1.99:6346/uri-res/N2R?" + hash), null);
        required = "X-Alt: " + pre + 99 + post;
        downloadInternal11(reqFile,
                         "Range: bytes=6-7",
                         out, in,
                         required);
                                 
        in.close();
        out.close();
        s.close();
    }
    
    public void testPrioritizingAlternates() throws Exception {

        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        RouterService.getAltlocManager().add(al, null);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: 1.1.1.1:1");
        assertTrue("alt failed", passed);
        // get rid of it.
        RouterService.getAltlocManager().remove(al, null);
        RouterService.getAltlocManager().remove(al, null);
        
        for(int i = 0; i < 50; i++) {
            al = AlternateLocation.create(
                "http://1.1.1." + i + ":6346/uri-res/N2R?" + hash);
            
            RouterService.getAltlocManager().add(al,null);
            
            //0-9, make as demoted.
            if( i < 10 ) {
                RouterService.getAltlocManager().remove(al,null); // should demote.
            }
            // 10-19, increment once.
            else if( i < 20 ) {
                RouterService.getAltlocManager().add(al, null); // should increment.
            }
            // 20-29, increment & demote.
            else if( i < 30 ) {
                RouterService.getAltlocManager().add(al, null); // increment
                RouterService.getAltlocManager().remove(al, null); // demote
            }
            // 30-39, increment twice.
            else if( i < 40 ) {
                RouterService.getAltlocManager().add(al, null); //increment
                RouterService.getAltlocManager().add(al, null); //increment
            }
            // 40-49, leave normal.
        }
        AlternateLocationCollection alc = RouterService.getAltlocManager().getDirect(FD.getSHA1Urn());
        assertEquals(50, alc.getAltLocsSize());
        
        //Order of return should be:
        // 40-49 returned first
        // 10-19 returned next
        // 30-39 returned next
        // 0-9 returned next
        // 20-29 returned next
        
        //1. Write request
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
            
        String reqFile = "/uri-res/N2R?" + hash;
        String pre = "1.1.1.";
        String post = "";
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = null;

        required = "X-Alt: " + 
                   pre + 40 + post + comma +
                   pre + 41 + post + comma +
                   pre + 42 + post + comma +    
                   pre + 43 + post + comma +
                   pre + 44 + post + comma +
                   pre + 45 + post + comma +
                   pre + 46 + post + comma +
                   pre + 47 + post + comma +
                   pre + 48 + post + comma +
                   pre + 49 + post;
        downloadInternal11(reqFile,
                         "Range: bytes=0-1",
                         out, in,
                         required);
                         
        required = "X-Alt: " + 
                   pre + 16 + post + comma +
                   pre + 13 + post + comma +
                   pre + 10 + post + comma +    
                   pre + 18 + post + comma +
                   pre + 15 + post + comma +
                   pre + 12 + post + comma +
                   pre + 17 + post + comma +
                   pre + 14 + post + comma +
                   pre + 11 + post + comma +
                   pre + 19 + post;
        downloadInternal11(reqFile,
                        "Range: bytes=2-3",
                        out, in,
                        required);
        
        required = "X-Alt: " + 
                   pre + 35 + post + comma +
                   pre + 32 + post + comma +
                   pre + 37 + post + comma +    
                   pre + 34 + post + comma +
                   pre + 31 + post + comma +
                   pre + 39 + post + comma +
                   pre + 36 + post + comma +
                   pre + 33 + post + comma +
                   pre + 30 + post + comma +
                   pre + 38 + post;
        downloadInternal11(reqFile,
                         "Range: bytes=4-5",
                         out, in,
                         required);
                         
        required = "X-Alt: " + 
                   pre + 5 + post + comma +
                   pre + 2 + post + comma +
                   pre + 7 + post + comma +    
                   pre + 4 + post + comma +
                   pre + 1 + post + comma +
                   pre + 9 + post + comma +
                   pre + 6 + post + comma +
                   pre + 3 + post + comma +
                   pre + 0 + post + comma +
                   pre + 8 + post;
        downloadInternal11(reqFile,
                         "Range: bytes=6-7",
                         out, in,
                         required);

        required = "X-Alt: " + 
                   pre + 24 + post + comma +
                   pre + 21 + post + comma +
                   pre + 29 + post + comma +    
                   pre + 26 + post + comma +
                   pre + 23 + post + comma +
                   pre + 20 + post + comma +
                   pre + 28 + post + comma +
                   pre + 25 + post + comma +
                   pre + 22 + post + comma +
                   pre + 27 + post;
        downloadInternal11(reqFile,
                         "Range: bytes=8-9",
                         out, in,
                         required);
                                 
        in.close();
        out.close();
        s.close();
    }      
    
/////////////////Miscellaneous tests for acceptable failure behaviour//////////
    public void testIncompleteFileUpload() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "HTTP/1.1 416 Requested Range Unavailable");
    }
    
    public void testIncompleteFileWithRanges() throws Exception {
        // add a range to the incomplete file.
        Interval iv = new Interval(50, 102500);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,"verifiedBlocks");
        vb.add(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-102499");
                
        // add another range and make sure we display it.
        iv = new Interval(150050, 252450);
        vb.add(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-102499, 150050-252449");
        
        // add an interval too small to report and make sure we don't report        
        iv = new Interval(102505, 150000);
        vb.add(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-102499, 150050-252449");
                
        // add the glue between the other intervals and make sure we condense
        // the ranges into a single larger range.
        iv = new Interval(102500, 102505);
        vb.add(iv);
        iv = new Interval(150000, 150050);
        vb.add(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-252449");
        
    }
    
    public void testIncompleteFileWithRangeRequest() throws Exception {
        String header = "Range: bytes 20-40";
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, header, true, true,
                 "HTTP/1.1 416 Requested Range Unavailable");
    }        

    public void testHTTP11WrongURI() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + badHash, null, true, true,
                "HTTP/1.1 404 Not Found");
    }
    
    public void testHTTP10WrongURI() throws Exception {
        // note that the header will be returned with 1.1
        // even though we sent with 1.0
        tFailureHeaderRequired(
            "/uri-res/N2R?" + badHash, null, false, false,
                "HTTP/1.1 404 Not Found");
    }
    
    public void testHTTP11MalformedURI() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + "no more school", null, true, false,
                "HTTP/1.1 400 Malformed Request");
    }
    
    public void testHTTP10MalformedURI() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + "no more school", null, false, false,
                "HTTP/1.1 400 Malformed Request");
    }
    
	public void testHTTP11MalformedGet() throws Exception {
        tFailureHeaderRequired(
            "/get/some/dr/pepper", null, true, false,
                "HTTP/1.1 400 Malformed Request");
    }
    
    public void testHTTP11MalformedHeader() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + hash, 
            "Range: 2-5", // expects "Range: bytes 2-5"
            true, false, "HTTP/1.1 400 Malformed Request");
    }

    ///////////////////test that creation time is given///////////
    public void testCreationTimeGiven() throws Exception {

        //0. Confirm creation time exists
        URN urn = URN.createSHA1Urn(hash);
        Long cTime = CreationTimeCache.instance().getCreationTime(urn);
        assertNotNull(cTime);
        assertTrue(cTime.longValue() > 0);

        assertTrue(download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                            "X-Create-Time: " + cTime));
    }
    
    
    public void testCreationTimeGivenForPartial() throws Exception {

        //0. make creatio time
        
        URN urn = URN.createSHA1Urn(incompleteHash);
        Long cTime = new Long("10776");
        CreationTimeCache.instance().addTime(urn, cTime.longValue());

        // successful interval 2-5 was set above
        assertTrue(download("/uri-res/N2R?" + incompleteHash, "Range: bytes 2-5",
                          "cdef", "X-Create-Time: " + cTime));
    }

    /////////////// test the feature header is used correctly ///////////
    public void testFeatureHeader() throws Exception {
        ChatSettings.CHAT_ENABLED.setValue(true);
        assertTrue(download(fileName, null, "abcdefghijklmnopqrstuvwxyz",
                   "X-Features: fwalt/0.1, browse/1.0, chat/0.1"));
                   
        ChatSettings.CHAT_ENABLED.setValue(false);
        assertTrue(download(fileName, null, "abcdefghijklmnopqrstuvwxyz",
                   "X-Features: fwalt/0.1, browse/1.0"));
    }
    
    /**
     * tests that the node sends a proper proxies header
     */
    public void testProxiesHeaderNotSent() throws Exception {
        
        // try when we are not firewalled
        PrivilegedAccessor.setValue(
                RouterService.getAcceptor(),
                "_acceptedIncoming",
                new Boolean(true));
        
        assertTrue(RouterService.acceptedIncomingConnection());
        
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
        
        assertFalse(
                containsHeader("GET",fileName,null,out,in,"X-Push-Proxy: 1.2.3.4:5"));
        
        try{in.close();}catch(IOException ignored){}
        try{out.close();}catch(IOException ignored){}
        
        Thread.sleep(1000);
        
        // now try with an empty set of proxies
        s = getSocketFromPush();
        in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
        
        in.readLine(); //skip GIV
		in.readLine(); //skip blank line
		
		PrivilegedAccessor.setValue(
                RouterService.getAcceptor(),
                "_acceptedIncoming",
                new Boolean(false));
        assertFalse(RouterService.acceptedIncomingConnection());
		
        assertFalse(
                containsHeader("GET",fileName,null,out,in,"X-Push-Proxy: 1.2.3.4:5"));
        
        try{in.close();}catch(IOException ignored){}
        try{out.close();}catch(IOException ignored){}
    }
    
    public void testProxiesHeaderSent() throws Exception{
        
        Socket s = getSocketFromPush();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        Writer out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
        
        in.readLine(); //skip GIV
		in.readLine(); //skip blank line
		
        // now try with some proxies
        ConnectionManager original = RouterService.getConnectionManager();
        
        final Set proxies = new TreeSet(IpPort.COMPARATOR);
        IpPort ppi = 
            new IpPortImpl("1.2.3.4",5);
        proxies.add(ppi);
        
        ConnectionManagerStub cmStub = new ConnectionManagerStub() {
            public Set getPushProxies() {
                return proxies;
            }
        };
        
        PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
        
        
		
		PrivilegedAccessor.setValue(
                RouterService.getAcceptor(),
                "_acceptedIncoming",
                new Boolean(false));
        assertFalse(RouterService.acceptedIncomingConnection());
		
        assertTrue(
                containsHeader("GET",fileName,null,out,in,"X-Push-Proxy: 1.2.3.4:5"));
        
        try{in.close();}catch(IOException ignored){}
        try{out.close();}catch(IOException ignored){}
        
        PrivilegedAccessor.setValue(RouterService.class,"manager",original);
                
    }
    
    //////////  test thex works /////////////
    public void testThexHeader() throws Exception {
        assertTrue(download(fileName, null, "abcdefghijklmnopqrstuvwxyz",
                "X-Thex-URI: " + "/uri-res/N2X?" + hash + ";" + ROOT32)
        );
    }
    
    public void testUploadFromBitprint() throws Exception {
        assertTrue(download("/uri-res/N2R?urn:bitprint:" +
            baseHash + "." + ROOT32, null,
             "abcdefghijklmnopqrstuvwxyz"));

        // we check for a valid bitprint length.
        tFailureHeaderRequired("/uri-res/N2R?urn:bitprint:" +
            baseHash + "." + "asdoihffd", null, true, false,
                "HTTP/1.1 400 Malformed Request");
             
        // but not for the valid base32 root -- in the future we may
        // and this test will break
        assertTrue(download("/uri-res/N2R?urn:bitprint:" +
            baseHash + "." + "SAMUWJUUSPLMMDUQZOWX32R6AEOT7NCCBX6AGBI", null,
             "abcdefghijklmnopqrstuvwxyz"));
             
        // make sure "bitprint:" is required for bitprint uploading.
        tFailureHeaderRequired("/uri-res/N2R?urn:sha1:" +
            baseHash + "." + ROOT32, null, true, false,
                "HTTP/1.1 400 Malformed Request");
    }
    
    public void testBadGetTreeRequest() throws Exception {
        tFailureHeaderRequired("/uri-res/N2X?" + badHash, null, true, true,
                "HTTP/1.1 404 Not Found");
                
        tFailureHeaderRequired("/uri-res/N2X?" + "no hash", null, true, false,
                "HTTP/1.1 400 Malformed Request");
    }
    
    public void testGetTree() throws Exception {
        byte[] dl = getDownloadBytes("/uri-res/N2X?" + hash, null, null);
//        assertEquals(FD.getHashTree().getOutputLength(), dl.length);
        DIMEParser parser = new DIMEParser(new ByteArrayInputStream(dl));
        parser.nextRecord(); // xml
        DIMERecord tree = parser.nextRecord();
        assertFalse(parser.hasNext());
        List allNodes = FD.getHashTree().getAllNodes();
        byte[] data = tree.getData();
        int offset = 0;
        for(Iterator genIter = allNodes.iterator(); genIter.hasNext(); ) {
            for(Iterator i = ((List)genIter.next()).iterator(); i.hasNext();) {
                byte[] current = (byte[])i.next();
                for(int j = 0; j < current.length; j++) {
                    assertEquals("offset: " + offset + ", idx: " + j,
                                 current[j], data[offset++]);
                }
            }
        }
        assertEquals(data.length, offset);
        // more extensive validity checks are in HashTreeTest
        // this is just checking to make sure we sent the right tree.
    }
                       
    
    /** 
     * Downloads file (stored in slot index) from address:port, returning the
     * content as a string. If header!=null, includes it as a request header.
     * Do not include new line or carriage return in header.  Throws IOException
     * if there is a problem, without cleaning up. 
     */
    private static boolean download1(String file,String header,String expResp,
                                     boolean uri) 
            throws IOException {
        //Unfortunately we can't use URLConnection because we need to test
        //malformed and slightly malformed headers

        //1. Write request
        boolean ret = true;
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
        String requestName;
        
        if( uri ) {
            requestName = "/uri-res/N2R?" + file;
        } else {
            requestName = "/get/" + index + "/" + file;
        }
        
        //first request with the socket
        String value=downloadInternal1("GET", requestName,
            header,out,in,expResp.length());

        ret = value.equals(expResp);//first request seccessful?
        //make second requst on same socket
        value = "";//reset
        
        value = downloadInternal1("GET", requestName,
            header, out, in, expResp.length());

        ret = ret && value.equals(expResp);//both reqests successful?
        in.close();
        out.close();
        s.close();
        return ret;
    }

    private static boolean download(String file,String header,String expResp) 
            throws IOException {
        return download(file, header, expResp, null, null);
    }
    
    private static boolean download(String file, String header, String expResp, Applyable f) 
            throws IOException {
        return download(file, header, expResp, null, f);
    }
    
    private static byte[] getDownloadBytes(String file, String header,
                                          String expheader)
      throws IOException {
        //1. Write request
        Socket s = new Socket("localhost", PORT);
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        String req = makeRequest(file);
        byte[] ret = getBytes("GET", req, header,
                             out, in, expheader, false, true);
        in.close();
        out.close();
        s.close();
        return ret;
    }

    private static boolean download(String file,String header,
                                    String expResp, String expHeader)
        throws IOException {
            return download(file, header, expResp, expHeader, null);
    }
    
    private static boolean download(String file, String header,
                                    String expResp, String expHeader, Applyable f) 
        throws IOException {
            
        //Unfortunately we can't use URLConnection because we need to test
        //malformed and slightly malformed headers
        
        //1. Write request
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));

        String ret=downloadInternal(file, header, out, in, expHeader, f);
        in.close();
        out.close();
        s.close();
        try {Thread.sleep(100); }catch(InterruptedException e){}
        return ret.equals(expResp);
    }


    /** Does a simple push GET download. */
    private static boolean downloadPush1(String indexedFile, String header, 
										 String expResp)
           		                         throws IOException, BadPacketException{
        return downloadPush1("GET", makeRequest(indexedFile), header, expResp);        
    }
    
    /**
     * Does a push & gets a socket from the incoming connection.
     */
    private static Socket getSocketFromPush() throws IOException, BadPacketException {
		Connection c = createConnection();
		c.initialize(new UltrapeerHeaders(null), new EmptyResponder(), 1000);
		QueryRequest query=QueryRequest.createQuery("txt", (byte)3);
        c.send(query);
        c.flush();
        QueryReply reply=null;
        for(int i = 0; i < 10; i++) {
            Message m=c.receive(2000);
            if (m instanceof QueryReply) {
                reply=(QueryReply)m;
                break;
            } 
        }

        if(reply == null)
            throw new IOException("didn't get query reply in time");

        PushRequest push =
            new PushRequest(GUID.makeGuid(),
                            (byte)3,
                            reply.getClientGUID(),
                            0,
                            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
                            callbackPort);

        //Create listening socket, then send push.
        ServerSocket ss=new ServerSocket(callbackPort);
        c.send(push);
        c.flush();
        ss.setSoTimeout(2000);
        Socket s = ss.accept();
        c.close();
        ss.close();
        return s;
    }
    
    /** 
     * Does an arbitrary push download. 
     * @param request an HTTP request such as "GET" or "HEAD
     * @param file the full filename, e.g., "/get/0/file.txt"    
     */
    private static boolean downloadPush1(String request,
                                         String file, String header, 
										 String expResp) 
        throws IOException, BadPacketException {
            //Establish push route
        Socket s = getSocketFromPush();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));

        in.readLine(); //skip GIV
        in.readLine(); //skip blank line
        
        //Download from the (incoming) TCP connection.
        String retStr=downloadInternal1(request, file, header, out, in,expResp.length());
        assertEquals("unexpected HTTP response message body", expResp, retStr);
        boolean ret = retStr.equals(expResp);
        
        // reset string variable
        retStr = downloadInternal1(request, file, header, out, in,expResp.length());
        assertEquals("unexpected HTTP response message body in second request", 
                     expResp, retStr);
        
        ret = ret && retStr.equals(expResp);
        
        //Cleanup
        s.close();
        return ret;
    }




    private static boolean downloadPush(String file, String header, 
										String expResp) 
            throws IOException, BadPacketException {
        Socket s = getSocketFromPush();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
		in.readLine(); //skip GIV
		in.readLine(); //skip blank line

        //Download from the (incoming) TCP connection.
        String retStr=downloadInternal(file, header, out, in);
		assertNotNull("string returned from download should not be null", retStr);
		
        //Cleanup
        s.close();
		assertEquals("wrong response", expResp, retStr);
        return retStr.equals(expResp);
    }

    /** 
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.
     * @param requiredHeader a header to look for, or null if we don't care
     */
    private static String downloadInternal(String file,
                                           String header,
                                           BufferedWriter out,
                                           BufferedReader in) 
    throws IOException {
        return downloadInternal(file, header, out, in, null, null);
    }
    
    /**
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.     
     * @param requestMethod the method to request ('GET', 'HEAD')
     * @param file the actual request (/get/ ...)
     * @param header the header to send, possibly null
     * @param out the writer
     * @param in the reader
     * @param requiredHeader the header we want to receive
     *      if null, then no header is expected.
     * @param http11 whether or not to write http 1.1 or 1.0
     * @return the contents of what we read
     */
    private static String downloadInternal(String requestMethod,
                                            String file,
                                            String header,
                                            BufferedWriter out,
                                            BufferedReader in,
                                            String requiredHeader,
                                            boolean http11,
                                            boolean require11Response,
                                            Applyable f)
      throws IOException {
        return new String(
          request(requestMethod, file, header, out, in, requiredHeader, http11, require11Response, f)
        );
    }
    
  private static byte[] getBytes(String requestMethod,
                                 String file,
                                 String header,
                                 OutputStream out,
                                 InputStream in,
                                 String requiredHeader,
                                 boolean http11,
                                 boolean require11Response)
     throws IOException {
        int length = readToContent(requestMethod, file, header,
                        new OutputStreamWriter(out),
                        in,
                        requiredHeader, http11, require11Response, null);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        //3. Read content.  Obviously this is designed for small files.
        for(int i = 0; i < length; i++) {
            int c = in.read();
            if (c < 0)
                break;
            bytes.write(c);
        }
        
        return bytes.toByteArray();
    }

    
    private static byte[] request(String requestMethod,
                                            String file,
                                            String header,
                                            BufferedWriter out,
                                            BufferedReader in,
                                            String requiredHeader,
                                            boolean http11,
                                            boolean require11Response,
                                            Applyable f)
     throws IOException {
        
        int length = readToContent(requestMethod, file, header, 
                                    out, in, requiredHeader, http11, require11Response, f);
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        //3. Read content.  Obviously this is designed for small files.
        for(int i = 0; i < length; i++) {
            int c = in.read();
            if (c < 0)
                break;
            bytes.write(c);
        }
        
        return bytes.toByteArray();
    }
        
    private static int readToContent(String requestMethod,
                                            String file,
                                            String header,
                                            Writer out,
                                            Object in,
                                            String requiredHeader,
                                            boolean http11,
                                            boolean require11Response,
                                            Applyable f)
     throws IOException {        
        // send request
        out.write( requestMethod + " " + file + " " + 
            (http11 ? "HTTP/1.1" : "HTTP/1.0") + "\r\n");
        if (header != null)
            out.write(header + "\r\n");
        if(http11)
            out.write("Connection: Keep-Alive\r\n");            
        out.write("\r\n");
        out.flush();

        //2. Read response code and headers, remember the content-length.
        boolean foundHeader = false;
        Header expectedHeader = null;
        int length = -1;
        
        if( requiredHeader != null )
            expectedHeader = new Header(requiredHeader);

        boolean firstLine = true;
        while (true) { 
            String line;
            if(in instanceof Reader)
                line = readLine((Reader)in);
            else
                line = readLine((InputStream)in);

            if(require11Response && firstLine && (line == null || !line.startsWith("HTTP/1.1")))
                fail("bad first response line: " + line);
            firstLine = false;
            
            if( line == null)
                throw new InterruptedIOException("connection closed");
                
            if (line.equals(""))
                break;

            if(f != null) {
                try {
                    f.apply(line);
                } catch(Exception e) {
                    fail(e);
                    return -1;
                }
            }
                            
            if (requiredHeader != null) {
                Header found = new Header(line);
                if( found.equals(expectedHeader)) {
                    foundHeader = true;
                }
            }
            if( line.startsWith("Content-Length: ") )
                length = Integer.valueOf(line.substring(15).trim()).intValue();            
        }

        //2A. If a header was required, make sure it was there.
        if (requiredHeader != null) {
			assertTrue("Didn't find header: " + requiredHeader, foundHeader);
		}
        
        //System.out.println("Download returns: " + System.currentTimeMillis());
		return length;
    }
    
    private static boolean containsHeader(String requestMethod,
                                            String file,
                                            String header,
                                            Writer out,
                                            Reader in,
                                            String requiredHeader)
    	throws IOException {
        // send request
        out.write( requestMethod + " " + makeRequest(file) + " " + 
            "HTTP/1.1\r\n");
        if (header != null)
            out.write(header + "\r\n");
        out.write("Connection: Keep-Alive\r\n");            
        out.write("\r\n");
        out.flush();

        //2. Read response code and headers, remember the content-length.
        Header expectedHeader = null;
        
        if( requiredHeader != null )
            expectedHeader = new Header(requiredHeader);
            
        while (true) { 
            String line = readLine(in);
            if( line == null)
                throw new InterruptedIOException("connection closed");
            //System.out.println("<< " + line);
                
            if (line.equals(""))
                break;
            if (requiredHeader != null) {
                Header found = new Header(line);
                if( found.title.equals(expectedHeader.title)) {
                    return true;
                }
            }
        }
        return false;
        
    }
    
    private static interface Applyable {
        public void apply(String line) throws Exception;
    }
    
    private static class Header {
        final String title;
        final List contents;
        
        public Header(String data) {
            contents = new LinkedList();
            int colon = data.indexOf(":");
            if( colon == -1 ) {
                title = data;
            } else {
                title = data.substring(0, colon);
                StringTokenizer st =
                    new StringTokenizer(data.substring(colon+1), ",");
                while(st.hasMoreTokens()) {
                    String info = st.nextToken().trim();
                    contents.add(info);
                }
            }       
        }
        
        public boolean equals(Object o) {
            if(o == this) return true;
            if(!(o instanceof Header)) return false;
            Header other = (Header)o;
            if(!title.toLowerCase().equals(other.title.toLowerCase()))
                return false;
            return listEquals(contents, other.contents);
        }
        
        public boolean listEquals(List one, List two) {
            if( one.size() != two.size() )
                return false;
            boolean found;
            for(Iterator i = one.iterator(); i.hasNext(); ) {
                found = false;
                String a = (String)i.next();
                for(Iterator j = two.iterator(); j.hasNext(); ) {
                    String b = (String)j.next();
                    if(a.equalsIgnoreCase(b))
                        found = true;
                }
                if(!found)
                    return false;
            }
            for(Iterator i = two.iterator(); i.hasNext(); ) {
                found = false;
                String a = (String)i.next();
                for(Iterator j = two.iterator(); j.hasNext(); ) {
                    String b = (String)j.next();
                    if(a.equalsIgnoreCase(b))
                        found = true;
                }
                if(!found)
                    return false;
            }
            return true;
        }

        public String toString() {
            return title + " : " + contents;
        }
    }
                
        
    
    
    /** 
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.
     * @param requiredHeader a header to look for, or null if we don't care
     */
    private static String downloadInternal11(String file,
                                           String header,
                                           BufferedWriter out,
                                           BufferedReader in,
                                           String requiredHeader) 
            throws IOException {
        return downloadInternal("GET", makeRequest(file), header,
                                    out, in, requiredHeader, true, true, null);
	}
    
    /** 
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.
     * @param requiredHeader a header to look for, or null if we don't care
     */
    private static String downloadInternal(String file,
                                           String header,
                                           BufferedWriter out,
                                           BufferedReader in,
                                           String requiredHeader,
                                           Applyable f) 
            throws IOException {
        // FIXME why send 1.0 header and expect 1.1?
//        return downloadInternal("GET", makeRequest(file), header,
//                                    out, in, requiredHeader, false, true, f);        
        return downloadInternal("GET", makeRequest(file), header,
              out, in, requiredHeader, false, false, f);
	}

    /** 
     * Sends an arbitrary request, returning the result.
     * @param file the full filename, e.g., "/get/0/file.txt"     
     * @param request an HTTP request such as "GET" or "HEAD"
     */
    private static String downloadInternal1(String request,
                                            String file,
											String header,
											BufferedWriter out,
											BufferedReader in,
											int expectedSize) 
        throws IOException {
        //Assert.that(out!=null && in!=null,"socket closed my server");
        //1. Send request
        out.write(request+" "+file+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection: Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();
        
        
        //2. Read (and ignore!) response code and headers.  TODO: verify.
        String firstLine = in.readLine();
        if(firstLine == null || !firstLine.startsWith("HTTP/1.1"))
            fail("bad first response line: " + firstLine);
                   
        while(!in.readLine().equals("")){ }
        //3. Read content.  Obviously this is designed for small files.
        StringBuffer buf=new StringBuffer();
        for(int i=0; i<expectedSize; i++) {
            int c = in.read();
            buf.append((char)c);
        }
        return buf.toString();
    }

    private static boolean pipelineDownloadNormal(String file, String header,
                                                  String expResp) 
        throws IOException {
        boolean ret = true;
        Socket s = new Socket("localhost",PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader
                                               (s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter
                                                (s.getOutputStream()));
        
        //write first request
        out.write("GET " + makeRequest(file)+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();
        
        //write second request 
        out.write("GET " + makeRequest(file) +" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();
        
        int expectedSize = expResp.length();
        
        //read...ignore response headers
        String firstLine = in.readLine();
        if(firstLine == null || !firstLine.startsWith("HTTP/1.1"))
            fail("bad first response line: " + firstLine);
        
        while(!in.readLine().equals("")){ }
        //read first response
        StringBuffer buf=new StringBuffer();        
        for(int i=0; i<expectedSize; i++){
            int c = in.read();
            buf.append((char)c);
        }
        ret = buf.toString().equals(expResp);
        //ingore second header
        buf = new StringBuffer();
        while(!in.readLine().equals("")){ }
        //read Second response
        for(int i=0; i<expectedSize; i++){
            int c = in.read();
            buf.append((char)c);
        }
        // close all the appropriate streams & sockets.
        in.close();
        out.close();
        s.close();
        return ret && buf.toString().equals(expResp);
    }

    private static boolean pipelineDownloadPush(String file, String 
                                                header, String expResp)
        throws IOException , BadPacketException {
        boolean ret = true;
        Socket s = getSocketFromPush();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
        in.readLine();  //skip GIV        
        in.readLine();  //skip blank line
        
        //write first request
        out.write("GET " + makeRequest(file) +" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();

        //write second request
        out.write("GET " + makeRequest(file) +" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();

        int expectedSize = expResp.length();
        
        //read...ignore response headers
        String firstLine = in.readLine();
        if(firstLine == null || !firstLine.startsWith("HTTP/1.1"))
            fail("bad first response line: " + firstLine);
            
        while(!in.readLine().equals("")){ }
        //read first response
        StringBuffer buf=new StringBuffer();        
        for(int i=0; i<expectedSize; i++){
            int c1 = in.read();
            buf.append((char)c1);
        }
        ret = buf.toString().equals(expResp);
        buf = new StringBuffer();
        
        //ingore second header
        firstLine = in.readLine();
        if(firstLine == null || !firstLine.startsWith("HTTP/1.1"))
            fail("bad first response line: " + firstLine);
        
        while(!in.readLine().equals("")){ }
        //read Second response
        for(int i=0; i<expectedSize; i++){
            int c1 = in.read();
            buf.append((char)c1);
        }
        // close all the appropriate streams & sockets.
        in.close();
        out.close();
        s.close();
        return ret && buf.toString().equals(expResp);
    }

    /** Makes sure that a HEAD request followed by a GET request does the right
     *  thing. */
    public void tMixedPersistentRequests() throws Exception {
        Socket s = null;
        try {
            //1. Establish connection.
            s = new Socket("localhost", PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                                                          s.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                                                          s.getOutputStream()));
            //2. Send HEAD request
            assertEquals("",
                downloadInternal1("HEAD", "/get/"+index+"/"+encodedFile, 
                                  null, out, in, 0));
            //3. Send GET request, make sure data ok.
            assertEquals(alphabet,
                downloadInternal1("GET", "/get/"+index+"/"+encodedFile,
                                  null, out, in, alphabet.length()));
        } finally {
            if (s!=null)
                try { s.close(); } catch (IOException ignore) { }
        }
    }

    /** Tests persistent connections with URI requests.  (Raphael Manfredi claimed this
     *  was broken.)  */
    public void tPersistentURIRequests() throws Exception {
        Socket s = null;
        try {
            //1. Establish connection.
            s = new Socket("localhost", PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                                                          s.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                                                          s.getOutputStream()));
            //2. Send GET request in URI form
            assertEquals(alphabet,
                         downloadInternal1("GET", "/uri-res/N2R?"+hash,
                                           null, out, in, alphabet.length()));
            //3. Send another GET request in URI form
            assertEquals(alphabet,
                         downloadInternal1("GET", "/uri-res/N2R?"+hash,
                                           null, out, in, alphabet.length()));
        } finally {
            if (s!=null)
                try { s.close(); } catch (IOException ignore) { }
        }
    }

    /**
     * Tests various cases for failed downloads, ensuring
     * that the correct header is sent back.
     */
    public void tFailureHeaderRequired(String file,
                                       String sendHeader,
                                       boolean http11,
                                       boolean repeat,
                                       String requiredHeader)
                                        throws Exception {
        Socket s = null;
        try {
            //1. Establish connection.
            s = new Socket("localhost", PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                                                          s.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                                                          s.getOutputStream()));
            //2. Send GET request in URI form
            downloadInternal("GET", file, sendHeader, out, in, 
                             requiredHeader, http11, true, null);
            
            //3. If the connection should remain open, make sure we
            //   can request again.
            if( repeat ) {
                downloadInternal("GET", file, sendHeader, out, in,
                                 requiredHeader, http11, true, null);
            } else {
                try {
                    downloadInternal("GET", file, sendHeader, out, in,
                                     requiredHeader, http11, false, null);
                    fail("Connection should be closed");
                } catch(InterruptedIOException good) {
                    // good.
                } catch(SocketException good) {
                    // good too.
                }
            }
            
        } finally {
            if (s!=null)
                try { s.close(); } catch (IOException ignore) { }
        }
    }

	/**
	 * Creates an Ultrapeer connection.
	 */
	private static Connection createConnection() {
		return new Connection("localhost", PORT);
	}
    

    private static class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                                         boolean outgoing) {
            return HandshakeResponse.createResponse(new Properties());
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
    
    private static class FManCallback extends ActivityCallbackStub {
        public void fileManagerLoaded() {
            synchronized(loaded) {
                loaded.notify();
            }
        }
    }

    private static void startAndWaitForLoad() {
        synchronized(loaded) {
            try {
                ROUTER_SERVICE.start();
                loaded.wait();
            } catch (InterruptedException e) {
                //good.
            }
        }
    }      
    
    private static String makeRequest(String req) {
        if(req.startsWith("/uri-res"))
            return req;
        else
            return "/get/" + index + "/" + req;
    }
    
    /** 
     * Reads a new line WITHOUT end of line characters.  A line is 
     * defined as a minimal sequence of character ending with "\n", with
     * all "\r"'s thrown away.  Hence calling readLine on a stream
     * containing "abc\r\n" or "a\rbc\n" will return "abc".
     *
     * Throws IOException if there is an IO error.  Returns null if
     * there are no more lines to read, i.e., EOF has been reached.
     * Note that calling readLine on "ab<EOF>" returns null.
     */
    public static String readLine(Reader _istream) throws IOException {
        if (_istream == null)
            return "";

		StringBuffer sBuffer = new StringBuffer();
        int c = -1; //the character just read
        boolean keepReading = true;
        
		do {
		    try {
			    c = _istream.read();
            } catch(ArrayIndexOutOfBoundsException aiooe) {
                // this is apparently thrown under strange circumstances.
                // interpret as an IOException.
                throw new IOException("aiooe.");
            }			    
			switch(c) {
			    // if this was a \n character, break out of the reading loop
			    case  '\n': keepReading = false;
			             break;
			    // if this was a \r character, ignore it.
			    case  '\r': continue;
			    // if we reached an EOF ...
			    case -1: return null;			             
                // if it was any other character, append it to the buffer.
			    default: sBuffer.append((char)c);
			}
        } while(keepReading);

		// return the string we have read.
		return sBuffer.toString();
    } 
   
    public static String readLine(InputStream in) throws IOException {
        return new ByteReader(in).readLine();
    }
    
    /**
     * testFALTNotRequested(), testFALTWhenRequested() and testFWALTWhenRequested()
     * fail if the server processes the entire request before we start reading
     * from the InputStreams. That means: our HTTPUploader is added to UploadManagers 
     * private _activeUploadsList, the request is processed and the HTTPUploader is
     * removed from the List. We start reading from the InputStream and the assertions
     * in the mentioned tests fail because our HTTPUploader is no longer in that
     * List. So, we have to cache the HTTPUploader somehow what this extension does.
     */
    private static class TestUploadManager extends HTTPUploadManager {

        private List activeUploads = new ArrayList();
        
        public TestUploadManager() {
        	super(new UploadSlotManager());
        }
        
        public synchronized void addAcceptedUploader(HTTPUploader uploader) {
            activeUploads.add(uploader);
            super.addAcceptedUploader(uploader);
        }
        
        public void clearUploads() {
            activeUploads.clear();
        }
    }
}
