package com.limegroup.gnutella.uploader;

import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.stubs.*;

/**
 * Test that a client uploads a file correctly.  Depends on a file
 * containing the lowercase characters a-z.
 */
public class UploadTest extends TestCase {
    private static String address;
    private static int port;
    /** The file name, plain and encoded. */
    private static String file="alphabet test file#2.txt";
    private static String encodedFile="alphabet%20test+file%232.txt";
    /** The file contents. */
	private static final String alphabet="abcdefghijklmnopqrstuvwxyz";
    /** The hash of the file contents. */
    private static final String hash="urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";
    private static final int index=0;
    /** Our listening port for pushes. */
    private static int callbackPort=6671;

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
		return new TestSuite(UploadTest.class);
	}

	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		try {
  			address = InetAddress.getLocalHost().getHostAddress();
  		} catch(UnknownHostException e) {
  			assertTrue("unexpected exception: "+e, false);
  		}
		port = 6668;
        //This assumes we're running in the limewire/tests directory
		File testDir = new File("com/limegroup/gnutella/uploader/data");
		if(!testDir.isDirectory()) {
			testDir = new File("com/limegroup/gnutella/uploader/data");
		}
		assertTrue("shared directory could not be found", testDir.isDirectory());
		assertTrue("test file should be in shared directory", 
				   new File(testDir, file).isFile());
		SettingsManager.instance().setPort(port);
        assertEquals(port, SettingsManager.instance().getPort());
		SettingsManager.instance().setDirectories(new File[] {testDir});
		SettingsManager.instance().setExtensions("txt");
		ConnectionSettings.KEEP_ALIVE.setValue(8);
		//SettingsManager.instance().setKeepAlive(8);
		SettingsManager.instance().setMaxUploads(10);
		SettingsManager.instance().setUploadsPerPerson(10);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
		//SettingsManager.instance().setConnectOnStartup(true);
        SettingsManager.instance().setFilterDuplicates(false);

		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        //SettingsManager.instance().setEverSupernodeCapable(true);
        //SettingsManager.instance().setForceSupernodeMode(true);
        //SettingsManager.instance().setDisableSupernodeMode(false);
		SettingsManager.instance().writeProperties();
		
		ActivityCallback callback = new ActivityCallbackStub();
		RouterService router = new RouterService(callback);
		router.start();
        assertEquals(port, SettingsManager.instance().getPort());

        //System.out.println(
        //    "Please make sure your client is listening on port "+port+"\n"
        //    +"of "+address+" and is sharing "+file+" in slot "+index+",\n"
        //    +"with at least one incoming messaging slot.  Also, nothing\n"
        //    +"may be listening to port "+callbackPort+".\n"
		//	+"Finally, the file must contain all lower-case characters in\n" 
		//	+"the alphabet, exactly like the following:\n\n"
		//	+"abcdefghijklmnopqrstuvwxyz");
		//System.out.println(); 
	}

    public void testAll() {
        try {
            boolean passed;

            //UploadTest works fine in isolation, but this sleep seems to be
            //needed to work as part of AllTests.  I'm not sure why.
            try {Thread.sleep(200); } catch (InterruptedException e) { }
            
            ///////////////////push downloads with HTTP1.0///////////
            passed = downloadPush(file, null,alphabet);
            assertTrue("Push download",passed);
            
            passed=downloadPush(encodedFile, null,alphabet);
            assertTrue("Push download, encoded file name",passed);

            passed=downloadPush(file, "Range: bytes=2-5","cdef");
            assertTrue("Push download, middle range, inclusive",passed);

            ///////////////////push downloads with HTTP1.1///////////////            
            passed = downloadPush1(file, null, alphabet);
            assertTrue("Push download with HTTP1.1",passed);
            
            passed=downloadPush1(encodedFile, null,
                                "abcdefghijklmnopqrstuvwxyz");
            assertTrue("Push download, encoded file name with HTTP1.1",passed);

            passed=downloadPush1(file, "Range: bytes=2-5","cdef");
            assertTrue("Push download, middle range, inclusive with HTTP1.1",passed);
            
            assertTrue("Persistent push HEAD requests", 
                       downloadPush1("HEAD", "/get/"+index+"/"+encodedFile, null, ""));
                                     
                       

            //////////////normal downloads with HTTP 1.0//////////////
            passed=download(file, null,"abcdefghijklmnopqrstuvwxyz");
            assertTrue("No range header",passed);
            
            passed=download(file, "Range: bytes=2-", 
                            "cdefghijklmnopqrstuvwxyz");
            assertTrue("Standard range header",passed);


            passed=download(file, "Range: bytes 2-", 
                            "cdefghijklmnopqrstuvwxyz");
            assertTrue("Range missing \"=\".  (Not legal HTTP, but common.)",
					   passed);


            passed=download(file, "Range: bytes=2-5","cdef",
                            "Content-range: bytes 2-5/26");
            assertTrue("Middle range, inclusive",passed);

        
            passed=download(file, "Range:bytes 2-",
                            "cdefghijklmnopqrstuvwxyz",
                            "Content-length:24");
            assertTrue("No space after \":\".  (Legal HTTP.)",passed);

            passed=download(file, "Range: bytes=-5","vwxyz");
            assertTrue("Last bytes of file",passed);

            passed=download(file, "Range: bytes=-30",
                            "abcdefghijklmnopqrstuvwxyz");
            assertTrue("Too big negative range request",passed);


            passed=download(file, "Range:   bytes=  2  -  5 ", "cdef");
            assertTrue("Lots of extra space",passed);


			assertEquals("Unexpected: "+URLDecoder.decode(encodedFile), file,
						 URLDecoder.decode(encodedFile));
            //Assert.that(URLDecoder.decode(encodedFile).equals(file),
			//          "Unexpected: "+URLDecoder.decode(encodedFile));
            passed=download(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
            assertTrue("URL encoded",passed);


            ////////////normal download with HTTP 1.1////////////////

            passed=download1(file, null,"abcdefghijklmnopqrstuvwxyz");
            assertTrue("No range header with HTTP1.1",passed);

            passed=download1(file, "Range: bytes=2-", 
                            "cdefghijklmnopqrstuvwxyz");
            assertTrue("Standard range header with HTTP1.1",passed);


            passed=download1(file, "Range: bytes 2-", 
                            "cdefghijklmnopqrstuvwxyz");
            assertTrue("Range missing \"=\". (Not legal HTTP, but common.)"+
                 "with HTTP1.1", passed);


            passed=download1(file, "Range: bytes=2-5","cdef");
            assertTrue("Middle range, inclusive with HTTP1.1",passed);

        
            passed=download1(file, "Range:bytes 2-",
                            "cdefghijklmnopqrstuvwxyz");
            assertTrue("No space after \":\".  (Legal HTTP.) with HTTP1.1",passed);

            passed=download1(file, "Range: bytes=-5","vwxyz");
            assertTrue("Last bytes of file with HTTP1.1",passed);


            passed=download1(file, "Range:   bytes=  2  -  5 ", "cdef");
            assertTrue("Lots of extra space with HTTP1.1",passed);


			assertEquals("Unexpected: "+URLDecoder.decode(encodedFile),
						 file, URLDecoder.decode(encodedFile));
            //Assert.that(URLDecoder.decode(encodedFile).equals(file),
			//          "Unexpected: "+URLDecoder.decode(encodedFile));
            passed=download1(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
            assertTrue("URL encoded with HTTP1.1",passed);
            
            //////////////////Pipelining tests with HTTP1.1////////////// 
            passed = pipelineDownloadNormal(file, null, 
                                            "abcdefghijklmnopqrstuvwxyz");
            assertTrue("piplining with normal download",passed);
            
            passed = pipelineDownloadPush(file,null, 
                                          "abcdefghijklmnopqrstuvwxyz");
            assertTrue("piplining with push download",passed);
         
            tMixedPersistentRequests();
            tPersistentURIRequests();

            System.out.println("passed");
        } catch (IOException e) {
            e.printStackTrace();
			assertTrue("unexpected exception: "+e, false);
        } catch (BadPacketException e) {
            e.printStackTrace();
			assertTrue("unexpected BadPacketException: "+e, false);
        }
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
        Socket s = new Socket(address, port);
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
        Socket s = new Socket(address, port);
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
     * @param request an HTTP request such as "GET" or "HEAD"
     * @param file the full filename, e.g., "/get/0/file.txt"    
     */
    private static boolean downloadPush1(String request,
                                         String file, String header, 
										 String expResp) 
		throws IOException, BadPacketException {
        //Establish push route
        Connection c=new Connection(address, port);
        c.initialize();
        QueryRequest query=new QueryRequest((byte)5, 0, "txt", false);
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
            (byte)5,
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
        Connection c=new Connection(address, port);
        c.initialize();
        QueryRequest query=new QueryRequest((byte)5, 0, "txt", false);
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
            (byte)5,
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
		assertTrue("should not have received the empty string", !retStr.equals(""));
		
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
     * @param requiredHeader a header to look for, or null if we don't care
     */
    private static String downloadInternal(String file,
                                           String header,
                                           BufferedWriter out,
                                           BufferedReader in,
                                           String requiredHeader) 
            throws IOException {
        //1. Send request
        out.write("GET /get/"+index+"/"+file+" HTTP/1.0\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("\r\n");
        out.flush();

        //2. Read (and ignore!) response code and headers.  TODO: verify.
        boolean foundHeader=false;
        while (true) { 
            String line = in.readLine();
            if(line.equals(""))
                break;
            if(requiredHeader!=null)
               if (canonicalizeHeader(line).
                       equals(canonicalizeHeader(requiredHeader)))
                   foundHeader = true;
        }
        if (requiredHeader!=null) {
            //TODO: convey this to the caller gently so it can print "FAILED"
			assertTrue("Didn't find header", foundHeader);
		}
        
        //3. Read content.  Obviously this is designed for small files.
        StringBuffer buf=new StringBuffer();
        while (true) {
            int c=in.read();
            if (c<0)
                break;
            buf.append((char)c);
        }
 
        return buf.toString();
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
        Socket s = new Socket(address,port);
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
        return ret && buf.toString().equals(expResp);
    }

    private static boolean pipelineDownloadPush(String file, String 
                                                header, String expResp)
        throws IOException , BadPacketException {
        boolean ret = true;
        //Establish push route
        Connection c=new Connection(address, port);
        c.initialize();
        QueryRequest query=new QueryRequest((byte)5, 0, "txt", false);
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
            (byte)5,
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
        return ret && buf.toString().equals(expResp);
    }

    /** Makes sure that a HEAD request followed by a GET request does the right
     *  thing. */
    public void tMixedPersistentRequests() {
        Socket s = null;
        try {
            //1. Establish connection.
            s = new Socket(address, port);
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
        } catch (IOException e) {
            e.printStackTrace();
            fail ("Mysterious IO problem: "+e);
        } finally {
            if (s!=null)
                try { s.close(); } catch (IOException ignore) { }
        }
    }

    /** Tests persistent connections with URI requests.  (Raphael Manfredi claimed this
     *  was broken.)  */
    public void tPersistentURIRequests() {
        Socket s = null;
        try {
            //1. Establish connection.
            s = new Socket(address, port);
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
        } catch (IOException e) {
            e.printStackTrace();
            fail ("Mysterious IO problem: "+e);
        } finally {
            if (s!=null)
                try { s.close(); } catch (IOException ignore) { }
        }
    }
}
