package com.limegroup.gnutella.uploader;

import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.altlocs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.Interval;

/**
 * Test that a client uploads a file correctly.  Depends on a file
 * containing the lowercase characters a-z.
 */
public class UploadTest extends BaseTestCase {
    private static String address;
    private static final int PORT = 6668;
    /** The file name, plain and encoded. */
    private static String testDirName = "com/limegroup/gnutella/uploader/data";
    private static String incName = "partial alphabet.txt";
    private static String fileName="alphabet test file#2.txt";
    private static String encodedFile="alphabet%20test+file%232.txt";
    /** The file contents. */
	private static final String alphabet="abcdefghijklmnopqrstuvwxyz";
    /** The hash of the file contents. */
    private static final String hash=   "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";
    private static final String badHash="urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";
    private static final String incompleteHash =
        "urn:sha1:INCOMCPLETEXBSQEZY37FIM5QQSA2OUJ";
    private static final int index=0;
    /** Our listening port for pushes. */
    private static final int callbackPort = 6671;
    private UploadManager upMan;
    /** The verifying file for the shared incomplete file */
    private static final VerifyingFile vf = new VerifyingFile(false);
    /** The filedesc of the shared file. */
    private FileDesc FD;    

    private static final RouterService ROUTER_SERVICE =
        new RouterService(new ActivityCallbackStub());

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
	private static void copyFile(File one, File two) throws Exception {
	    FileInputStream fis = new FileInputStream(one);
	    FileOutputStream fos = new FileOutputStream(two);
	    int read = fis.read();
	    while(read != -1) {
	        fos.write(read);
	        read = fis.read();
	    }
    }

	protected void setUp() throws Exception {
		address = InetAddress.getLocalHost().getHostAddress();
		FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
		    new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);

        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt");
        UploadSettings.MAX_UPLOADS.setValue(10);
		UploadSettings.UPLOADS_PER_PERSON.setValue(10);

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

        if ( !ROUTER_SERVICE.isStarted() ) {
            ROUTER_SERVICE.start();
            Thread.sleep(2000);
        }
	    
        assertEquals("ports should be equal",
                     PORT, ConnectionSettings.PORT.getValue());
                     
        upMan = RouterService.getUploadManager();
        
        FileManager fm = RouterService.getFileManager();
        File incFile = new File(_incompleteDir, incName);
        CommonUtils.copyResourceFile(testDirName + "/" + incName, incFile);
        URN urn = URN.createSHA1Urn(incompleteHash);
        Set urns = new HashSet();
        urns.add(urn);
        fm.addIncompleteFile(
            incFile,
            urns,
            incName,
            1981,
            vf );
        assertEquals( 1, fm.getNumIncompleteFiles() );
        assertEquals( 1, fm.getNumFiles() );
        FD = fm.getFileDescForFile(new File(_sharedDir, fileName));
        // remove all alts for clarity.
        List alts = new LinkedList();
        AlternateLocationCollection alc = FD.getAlternateLocationCollection();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add((AlternateLocation)i.next());
        for(Iterator i = alts.iterator(); i.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)i.next();
            alc.remove(al); // demote
            alc.remove(al); // remove
        }

        try {Thread.sleep(300); } catch (InterruptedException e) { }
        //System.out.println(
        //    "Please make sure your client is listening on port "+port+"\n"
        //    +"of "+address+" and is sharing "+file+" in slot "+index+",\n"
        //    +"with at least one incoming messaging slot.  Also, nothing\n"
        //    +"may be listening to port "+callbackPort+".\n"
		//	+"Finally, the file must contain all lower-case characters in\n" 
		//	+"the alphabet, exactly like the following:\n\n"
		//	+"abcdefghijklmnopqrstuvwxyz");
		//System.out.println();
		
		assertEquals("unexpected uploads in progress",
		    0, upMan.uploadsInProgress() );
        assertEquals("unexpected queued uploads",
            0, upMan.getNumQueuedUploads() );
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

    public void testHTTP10DownloadRangeNoSpace() throws Exception {
        boolean passed = false;
        passed =download(fileName, "Range:bytes 2-",
                    "cdefghijklmnopqrstuvwxyz",
                    "Content-length:24");
        assertTrue("No space after \":\".  (Legal HTTP.)",passed);
    }

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
        assertEquals("Unexpected: "+java.net.URLDecoder.decode(encodedFile), fileName,
                     java.net.URLDecoder.decode(encodedFile));
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
        vf.addInterval(iv);
        passed = download1(incompleteHash, "Range: bytes 2-5", "cdef", true);
        assertTrue("incomplete range did not work", passed);
    }
    
    public void testHTTP11DownloadURLEncoding() throws Exception {
        boolean passed = false;
        
        assertEquals("URLDecoder broken",
            fileName, java.net.URLDecoder.decode(encodedFile));

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
    
    // Tests for alternate locations
    public void testAlternateLocationAddAndRemove() throws Exception {
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        FD.add(al);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        
        // Ensure that one removal doesn't stop it.
        FD.remove(al);
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        
        //Add a second one, so we can check to make sure
        //another removal removes the first one.
        String loc2 = "http://2.2.2.2:2/uri-res/N2R?" + hash;
        AlternateLocation al2 = AlternateLocation.create(loc2);
        FD.add(al2);
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc2 + ", " + loc );
        assertTrue("alt failed", passed);
        
        //Remove the first guy again, should only have loc2 left.
        FD.remove(al);
        passed = download("/uri-res/N2R?" + hash,null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc2 );
        assertTrue("alt failed", passed);
    }
    
    //Tests that headers the downloader gives are used.
    public void testSentHeaderIsUsed() throws Exception {
        AlternateLocationCollection alc = FD.getAlternateLocationCollection();
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        FD.add(al);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        
        // Add a header that gives a new location.
        String sendLoc = "http://2.2.2.2:2/uri-res/N2R?" + hash;
        AlternateLocation sendAl = AlternateLocation.create(sendLoc);
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // Make sure the FD has that loc now.
        assertEquals("wrong # locs", 2, FD.getAltLocsSize());
        List alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );
        
        //Make sure a request will give us both locs now.
        passed = download("/uri-res/N2R?" + hash,
                          null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + sendLoc + ", " + loc);
        assertTrue("alt failed", passed);
        
        //Demote the location (don't remove)
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // Should still have it.
        assertEquals("wrong # locs", 2, FD.getAltLocsSize());
        alts = new LinkedList();
        for(Iterator i = alc.iterator(); i.hasNext(); )
            alts.add(i.next());
        assertTrue( alts.contains(al) );
        assertTrue( alts.contains(sendAl) );
        
        //Now remove.
        passed = download("/uri-res/N2R?" + hash,
                          "X-NAlt: " + sendLoc,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // Now is removed.
        assertEquals("wrong # locs", 1, FD.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }
    
    // unfortunately we can't test with private addresses
    // because all these connections require that local_is_private
    // is false, which turns off isPrivateAddress checking.
    public void testInvalidAltsAreIgnored() throws Exception {
        AlternateLocationCollection alc = FD.getAlternateLocationCollection();
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        FD.add(al);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        
        // Add an invalid alt
        String invalidAddr = "http://0.0.0.0:6346/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidAddr,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, FD.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
        
        invalidAddr = "http://255.255.255.255:6346/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidAddr,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, FD.getAltLocsSize());
        assertEquals(al, alc.iterator().next());

        // Add an invalid port
        String invalidPort = "http://1.2.3.4:0/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidPort,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, FD.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
        
        invalidPort = "http://1.2.3.4:-1/uri-res/N2R?" + hash;
        passed = download("/uri-res/N2R?" + hash,
                          "X-Alt: " + invalidPort,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc);
        assertTrue("alt failed", passed);
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, FD.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }
    
    public void test10AltsAreSent() throws Exception {
        AlternateLocationCollection alc = FD.getAlternateLocationCollection();
        
        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        FD.add(al);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        
        for(int i = 0; i < 20; i++) {
            FD.add( AlternateLocation.create(
                "http://1.1.1." + i + ":6346/uri-res/N2R?" + hash));
        }
        assertEquals(21, alc.getAltLocsSize());
    
        String pre = "http://1.1.1.";
        String post = ":6346/uri-res/N2R?" + hash;
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
                           pre + "1:1/uri-res/N2R?" + hash;
        
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          required);
        assertTrue(passed);
    }
    
    public void testChunksGiveDifferentLocs() throws Exception {
        AlternateLocationCollection alc = FD.getAlternateLocationCollection();

        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        FD.add(al);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        
        for(int i = 0; i < 20; i++) {
            FD.add( AlternateLocation.create(
                "http://1.1.1." + i + ":6346/uri-res/N2R?" + hash));
        }
        assertEquals(21, alc.getAltLocsSize());        
        
        //1. Write request
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
            
        String reqFile = "/uri-res/N2R?" + hash;
        String pre = "http://1.1.1.";
        String post = ":6346/uri-res/N2R?" + hash;
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
                   pre + "1:1/uri-res/N2R?" + hash;
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
        FD.add( AlternateLocation.create(
                "http://1.1.1.99:6346/uri-res/N2R?" + hash));
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
        AlternateLocationCollection alc = FD.getAlternateLocationCollection();

        // Add a simple marker alt so we know it only contains that
        String loc = "http://1.1.1.1:1/uri-res/N2R?" + hash;
        AlternateLocation al = AlternateLocation.create(loc);
        FD.add(al);
        boolean passed = false;
        passed = download("/uri-res/N2R?" + hash, null,
                          "abcdefghijklmnopqrstuvwxyz",
                          "X-Alt: " + loc );
        assertTrue("alt failed", passed);
        // get rid of it.
        FD.remove(al);
        FD.remove(al);
        
        for(int i = 0; i < 50; i++) {
            al = AlternateLocation.create(
                "http://1.1.1." + i + ":6346/uri-res/N2R?" + hash);
            
            FD.add(al);
            
            //0-9, make as demoted.
            if( i < 10 ) {
                FD.remove(al); // should demote.
            }
            // 10-19, increment once.
            else if( i < 20 ) {
                FD.add(al); // should increment.
            }
            // 20-29, increment & demote.
            else if( i < 30 ) {
                FD.add(al); // increment
                FD.remove(al); // demote
            }
            // 30-39, increment twice.
            else if( i < 40 ) {
                FD.add(al); //increment
                FD.add(al); //increment
            }
            // 40-49, leave normal.
        }
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
        String pre = "http://1.1.1.";
        String post = ":6346/uri-res/N2R?" + hash;
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = null;

        required = "X-Alt: " + 
                   pre + 46 + post + comma +
                   pre + 43 + post + comma +
                   pre + 40 + post + comma +    
                   pre + 48 + post + comma +
                   pre + 45 + post + comma +
                   pre + 42 + post + comma +
                   pre + 47 + post + comma +
                   pre + 44 + post + comma +
                   pre + 41 + post + comma +
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
        vf.addInterval(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-102499");
                
        // add another range and make sure we display it.
        iv = new Interval(150050, 252450);
        vf.addInterval(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-102499, 150050-252449");
        
        // add an interval too small to report and make sure we don't report        
        iv = new Interval(102505, 150000);
        vf.addInterval(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-102499, 150050-252449");
                
        // add the glue between the other intervals and make sure we condense
        // the ranges into a single larger range.
        iv = new Interval(102500, 102505);
        vf.addInterval(iv);
        iv = new Interval(150000, 150050);
        vf.addInterval(iv);
        tFailureHeaderRequired(
            "/uri-res/N2R?" + incompleteHash, null, true, true,
                "X-Available-Ranges: bytes 50-252449");
        
    }
    
    public void testIncompleteFileWithRangeRequest() throws Exception {
        String header = "Range: bytes 30-50";
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
        return download(file, header, expResp, null);
    }

    private static boolean download(String file,String header,
                                    String expResp, String expHeader)
        throws IOException {
        //Unfortunately we can't use URLConnection because we need to test
        //malformed and slightly malformed headers
        
        //1. Write request
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));

        String ret=downloadInternal(file, header, out, in, expHeader);
        in.close();
        out.close();
        s.close();
        return ret.equals(expResp);
    }


    /** Does a simple push GET download. */
    private static boolean downloadPush1(String indexedFile, String header, 
										 String expResp)
           		                         throws IOException, BadPacketException{
        return downloadPush1("GET", "/get/"+index+"/"+indexedFile, header, expResp);        
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
		Connection c = createConnection();
		c.initialize();
		QueryRequest query=QueryRequest.createQuery("txt", (byte)3);
            c.send(query);
            c.flush();
            QueryReply reply=null;
            while (true) {
                Message m=c.receive(2000);
                if (m instanceof QueryReply) {
                    reply=(QueryReply)m;
                    break;
                } 
            }
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
            Socket s=ss.accept();
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
            c.close();
            s.close();
            ss.close();        
            return ret;
    }




    private static boolean downloadPush(String file, String header, 
										String expResp) 
            throws IOException, BadPacketException {
        //Establish push route
		Connection c = createConnection();
        c.initialize();
        QueryRequest query = QueryRequest.createQuery("txt", (byte)3);
        c.send(query);
        c.flush();
        QueryReply reply=null;
        while (true) {
            Message m=c.receive(2000);
            if (m instanceof QueryReply) {
                reply=(QueryReply)m;
                break;
            } 
        }
		assertNotNull("reply should not be null", reply);
        PushRequest push=new PushRequest(GUID.makeGuid(),
            (byte)3,
            reply.getClientGUID(),
            0,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
            callbackPort);

        //Create listening socket, then send push.
        ServerSocket ss=new ServerSocket(callbackPort);
        c.send(push);
        c.flush();
        Socket s=ss.accept();
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
        c.close();
        s.close();
        ss.close();    
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
        return downloadInternal(file, header, out, in, null);
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
                                            boolean http11)
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
        String expectedHeader = null;
        int length = -1;
        
        if( requiredHeader != null )
            expectedHeader = canonicalizeHeader(requiredHeader);
            
        while (true) { 
            String line = in.readLine();
            if( line == null )
                throw new InterruptedIOException("connection closed");
            System.out.println(line);
            if (line.equals(""))
                break;
            if (requiredHeader != null) {
                if (canonicalizeHeader(line).equals(expectedHeader)) {
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
        
        //3. Read content.  Obviously this is designed for small files.
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < length; i++) {
            int c = in.read();
            if (c < 0)
                break;
            buf.append((char)c);
        }
        
        return buf.toString();
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
        return downloadInternal("GET", "/get/" + index + "/" + file, header,
                                    out, in, requiredHeader, true);
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
                                           String requiredHeader) 
            throws IOException {
        return downloadInternal("GET", "/get/" + index + "/" + file, header,
                                    out, in, requiredHeader, false);
	}
    
    
    /** Removes all spaces and lower cases all characters. */
    private static String canonicalizeHeader(String line) {
        //Although we really should not replace 'bytes 2' with 'bytes2'
        //this method does...but who cares?
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<line.length(); i++) {
            if (line.charAt(i)!=' ')
                buf.append(Character.toLowerCase(line.charAt(i)));
        }
        return buf.toString();
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
        out.write("GET /get/"+index+"/"+file+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();
        
        //write second request 
        out.write("GET /get/"+index+"/"+file+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();
        
        int expectedSize = expResp.length();
        
        //read...ignore response headers
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
        //Establish push route
		Connection c = createConnection();
        c.initialize();
        QueryRequest query=QueryRequest.createQuery("txt", (byte)3);
        c.send(query);
        c.flush();
        QueryReply reply=null;
        while (true) {
            Message m=c.receive(2000);
            if (m instanceof QueryReply) {
                reply=(QueryReply)m;
                break;
            } 
        }
        PushRequest push=new PushRequest(GUID.makeGuid(),
            (byte)3,
            reply.getClientGUID(),
            0,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
            callbackPort);

        //Create listening socket, then send push.
        ServerSocket ss=new ServerSocket(callbackPort);
        c.send(push);
        c.flush();
        Socket s=ss.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
        in.readLine();  //skip GIV        
        in.readLine();  //skip blank line
        
        //write first request
        out.write("GET /get/"+index+"/"+file+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();

        //write second request
        out.write("GET /get/"+index+"/"+file+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
        out.write("\r\n");
        out.flush();

        int expectedSize = expResp.length();
        
        //read...ignore response headers
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
        ss.close();
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
                             requiredHeader, http11);
            
            //3. If the connection should remain open, make sure we
            //   can request again.
            if( repeat ) {
                downloadInternal("GET", file, sendHeader, out, in,
                                 requiredHeader, http11);
            } else {
                try {
                    downloadInternal("GET", file, sendHeader, out, in,
                                     requiredHeader, http11);
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
		return new Connection("localhost", PORT, new UltrapeerProperties(),
							  new EmptyResponder());
	}

    private static class UltrapeerProperties extends Properties {
        public UltrapeerProperties() {
            put(HeaderNames.USER_AGENT, CommonUtils.getHttpServer());
            put(HeaderNames.X_QUERY_ROUTING, "0.1");
            put(HeaderNames.X_ULTRAPEER, "true");
            put(HeaderNames.GGEP, "1.0");  //just for fun
        }
    }
    

    private static class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                                         boolean outgoing) throws IOException {
            return HandshakeResponse.createResponse(new Properties());
        }
    }
}
