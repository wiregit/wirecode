package com.limegroup.gnutella.uploader;

import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;

/**
 * Test that a client uploads a file correctly.  Depends on a file
 * containing the lowercase characters a-z.
 */
public class UploadTest extends com.limegroup.gnutella.util.BaseTestCase {
    private static String address;
    private static final int PORT = 6668;
    /** The file name, plain and encoded. */
    private static String file="alphabet test file#2.txt";
    private static String encodedFile="alphabet%20test+file%232.txt";
    /** The file contents. */
	private static final String alphabet="abcdefghijklmnopqrstuvwxyz";
    /** The hash of the file contents. */
    private static final String hash=   "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";
    private static final String badHash="urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";
    private static final int index=0;
    /** Our listening port for pushes. */
    private static final int callbackPort = 6671;
    private UploadManager upMan;

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

	protected void setUp() throws Exception {
		address = InetAddress.getLocalHost().getHostAddress();
        SettingsManager.instance().setBannedIps(new String[] {"*.*.*.*"});
        SettingsManager.instance().setAllowedIps(new String[] {"127.*.*.*"});
		SettingsManager.instance().setPort(PORT);
        //This assumes we're running in the limewire/tests directory
		File testDir = CommonUtils.getResourceFile("com/limegroup/gnutella/uploader/data");
		assertTrue("shared directory could not be found", testDir.isDirectory());
		assertTrue("test file should be in shared directory", 
				   new File(testDir, file).isFile());
		SettingsManager.instance().setDirectories(new File[] {testDir});
		SettingsManager.instance().setExtensions("txt");
		SettingsManager.instance().setMaxUploads(10);
		UploadSettings.UPLOADS_PER_PERSON.setValue(10);

        SettingsManager.instance().setFilterDuplicates(false);


		ConnectionSettings.NUM_CONNECTIONS.setValue(8);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

        if ( !ROUTER_SERVICE.isStarted() )
            ROUTER_SERVICE.start();			    
	    
        assertEquals("ports should be equal",
                     PORT, SettingsManager.instance().getPort());
                     
        upMan = RouterService.getUploadManager();


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
        passed = downloadPush(file, null,alphabet);
        assertTrue("Push download",passed);
    }

    public void testHTTP10PushEncodedFile() throws Exception {
        boolean passed = false;        
        passed=downloadPush(encodedFile, null,alphabet);
        assertTrue("Push download, encoded file name",passed);
    }

    public void testHTTP10PushRange() throws Exception {
        boolean passed = false;
        passed =downloadPush(file, "Range: bytes=2-5","cdef");
        assertTrue("Push download, middle range, inclusive",passed);
    }

    ///////////////////push downloads with HTTP1.1///////////////            
    public void testHTTP11Push() throws Exception {
        boolean passed = false;
        passed = downloadPush1(file, null, alphabet);
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
        passed =downloadPush1(file, "Range: bytes=2-5","cdef");
        assertTrue("Push download, middle range, inclusive with HTTP1.1",passed);
    }
     

    public void testHTTP11Head() throws Exception {
        assertTrue("Persistent push HEAD requests", 
                   downloadPush1("HEAD", "/get/"+index+"/"+encodedFile, null, ""));
    }
        
                       

    //////////////normal downloads with HTTP 1.0//////////////

    public void testHTTP10Download() throws Exception {
        boolean passed = false;
        passed =download(file, null,"abcdefghijklmnopqrstuvwxyz");
        assertTrue("No range header",passed);
    }
    
    public void testHTTP10DownloadRange() throws Exception {
        boolean passed = false;
        passed =download(file, "Range: bytes=2-", 
                    "cdefghijklmnopqrstuvwxyz");
        assertTrue("Standard range header",passed);
    }

    public void testHTTP10DownloadMissingRange() throws Exception {
        boolean passed = false;
        passed =download(file, "Range: bytes 2-", 
                    "cdefghijklmnopqrstuvwxyz");
        assertTrue("Range missing \"=\".  (Not legal HTTP, but common.)",
               passed);
    }

    public void testHTTP10DownloadMiddleRange() throws Exception {
        boolean passed = false;
        passed =download(file, "Range: bytes=2-5","cdef",
                    "Content-range: bytes 2-5/26");
        assertTrue("Middle range, inclusive",passed);
    }

    public void testHTTP10DownloadRangeNoSpace() throws Exception {
        boolean passed = false;
        passed =download(file, "Range:bytes 2-",
                    "cdefghijklmnopqrstuvwxyz",
                    "Content-length:24");
        assertTrue("No space after \":\".  (Legal HTTP.)",passed);
    }

    public void testHTTP10DownloadRangeLastByte() throws Exception {
        boolean passed = false;
        passed =download(file, "Range: bytes=-5","vwxyz");
        assertTrue("Last bytes of file",passed);
    }

    public void testHTTP10DownloadRangeTooBigNegative() throws Exception {
        boolean passed = false;
        passed =download(file, "Range: bytes=-30",
                    "abcdefghijklmnopqrstuvwxyz");
        assertTrue("Too big negative range request",passed);
    }


    public void testHTTP10DownloadRangeExtraSpace() throws Exception {
        boolean passed = false;
        passed =download(file, "Range:   bytes=  2  -  5 ", "cdef");
        assertTrue("Lots of extra space",passed);
    }


    public void testHTTP10DownloadURLEncoding() throws Exception {
        assertEquals("Unexpected: "+java.net.URLDecoder.decode(encodedFile), file,
                     java.net.URLDecoder.decode(encodedFile));
        boolean passed = false;
        passed =download(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
        assertTrue("URL encoded",passed);
    }

    ////////////normal download with HTTP 1.1////////////////

    public void testHTTP11DownloadNoRangeHeader() throws Exception {
        boolean passed = false;
        passed =download1(file, null,"abcdefghijklmnopqrstuvwxyz");
        assertTrue("No range header with HTTP1.1",passed);
    }

    public void testHTTP11DownloadStandardRangeHeader() throws Exception {
        boolean passed = false;
        passed =download1(file, "Range: bytes=2-", 
                     "cdefghijklmnopqrstuvwxyz");
        assertTrue("Standard range header with HTTP1.1",passed);
    }


    public void testHTTP11DownloadRangeMissingEquals() throws Exception {
        boolean passed = false;
        passed =download1(file, "Range: bytes 2-", 
                     "cdefghijklmnopqrstuvwxyz");
        assertTrue("Range missing \"=\". (Not legal HTTP, but common.)"+
               "with HTTP1.1", passed);
    }

    public void testHTTP11DownloadMiddleRange() throws Exception {
        boolean passed = false;
        passed =download1(file, "Range: bytes=2-5","cdef");
        assertTrue("Middle range, inclusive with HTTP1.1",passed);
    }
        
    public void testHTTP11DownloadRangeNoSpaceAfterColon() throws Exception {
        boolean passed = false;
        passed =download1(file, "Range:bytes 2-",
                     "cdefghijklmnopqrstuvwxyz");
        assertTrue("No space after \":\".  (Legal HTTP.) with HTTP1.1",passed);
    }

    public void testHTTP11DownloadRangeLastByte() throws Exception {
        boolean passed = false;
        passed =download1(file, "Range: bytes=-5","vwxyz");
        assertTrue("Last bytes of file with HTTP1.1",passed);
    }


    public void testHTTP11DownloadRangeLotsOfExtraSpace() throws Exception {
        boolean passed = false;
        passed =download1(file, "Range:   bytes=  2  -  5 ", "cdef");
        assertTrue("Lots of extra space with HTTP1.1",passed);        

        
        assertEquals("Unexpected: "+java.net.URLDecoder.decode(encodedFile),
                     file, java.net.URLDecoder.decode(encodedFile));
    }

    public void testHTTP11DownloadURLEncoding() throws Exception {
        boolean passed = false;
        passed =download1(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
        assertTrue("URL encoded with HTTP1.1",passed);

    }

//////////////////Pipelining tests with HTTP1.1//////////////             
    public void testHTTP11PipeliningDownload() throws Exception {
        boolean passed = false;
        passed = pipelineDownloadNormal(file, null, 
                                    "abcdefghijklmnopqrstuvwxyz");
        assertTrue("piplining with normal download",passed);
    }
            
    public void testHTTP11PipeliningDownloadPush() throws Exception {
        boolean passed = false;
        passed = pipelineDownloadPush(file,null, 
                                       "abcdefghijklmnopqrstuvwxyz");
        assertTrue("piplining with push download",passed);
    }
         
    public void testHTTP11DownloadMixedPersistent() throws Exception {
        tMixedPersistentRequests();
    }

    public void testHTTP11DownloadPersistentURI() throws Exception {
        tPersistentURIRequests();
    }
    
/////////////////Miscellaneous tests for acceptable failure behaviour//////////
    public void testHTTP11WrongURI() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + badHash, true, "HTTP/1.1 404 Not Found");
    }
    
    public void testHTTP10WrongURI() throws Exception {
        // note that the header will be returned with 1.1
        // even though we sent with 1.0
        tFailureHeaderRequired(
            "/uri-res/N2R?" + badHash, false, "HTTP/1.1 404 Not Found");
    }
    
    public void testHTTP11MalformedURI() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + "no more school", true,
            "HTTP/1.1 400 Malformed Request");
    }
    
    public void testHTTP10MalformedURI() throws Exception {
        tFailureHeaderRequired(
            "/uri-res/N2R?" + "no more school", false,
            "HTTP/1.1 400 Malformed Request");
    }
    
	public void testHTTP11MalformedGet() throws Exception {
        tFailureHeaderRequired(
            "/get/some/dr/pepper", true, "HTTP/1.1 400 Malformed Request");
    }

    /** 
     * Downloads file (stored in slot index) from address:port, returning the
     * content as a string. If header!=null, includes it as a request header.
     * Do not include new line or carriage return in header.  Throws IOException
     * if there is a problem, without cleaning up. 
     */
    private static boolean download1(String file,String header,String expResp) 
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
        //first request with the socket
        String value=downloadInternal1(file,header,out,in,expResp.length());
        //System.out.println("Sumeet: first return value "+value);
        ret = value.equals(expResp);//first request seccessful?
        //make second requst on same socket
        value = "";//reset
        value = downloadInternal1(file, header, out, in, expResp.length());
        //System.out.println("Sumeet: first return value "+value);
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
		assertNotEquals("should not have received the empty string", "", retStr);
		
        //Cleanup
        c.close();
        s.close();
        ss.close();    
		assertEquals("was "+retStr+" instead of "+expResp, expResp, retStr);
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
            if (line == null || line.equals(""))
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
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.
     * @param indexedFile a partially qualified name, e.g. "file.txt".  The 
     * "/get/<index>" is automatically appended
     */
    private static String downloadInternal1(String indexedFile,
											String header,
											BufferedWriter out,
											BufferedReader in,
											int expectedSize) 
                                            throws IOException {
        return downloadInternal1("GET", "/get/"+index+"/"+indexedFile, header, 
                                 out, in, expectedSize);
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
        for(int i=0; i<expectedSize; i++){
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
                downloadInternal1(encodedFile, null, out, in, alphabet.length()));
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
                                boolean http11,
                                String requiredHeader) throws Exception {
        Socket s = null;
        try {
            //1. Establish connection.
            s = new Socket("localhost", PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                                                          s.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                                                          s.getOutputStream()));
            //2. Send GET request in URI form
            downloadInternal("GET", file, null, out, in, 
                             requiredHeader, http11);
            
            //3. If we're testing HTTP1.1 then send another request
            //   to make sure that failures keep the connection alive
            //   if requested.
            if( http11 ) {
                downloadInternal("GET", file, null, out, in,
                                 requiredHeader, http11);
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
