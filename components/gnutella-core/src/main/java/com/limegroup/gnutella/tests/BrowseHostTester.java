package com.limegroup.gnutella.tests;

import java.io.*;
import java.util.*;
import java.net.*;
import junit.framework.*;
import com.limegroup.gnutella.*;

public class BrowseHostTester extends TestCase {

    public static final int LW_PORT = 6000;

    private static byte[] serventID = null;

    public BrowseHostTester(String name) {
        super(name);
    }
    
    protected void setUp() {
        debug("BrowseHostTester.setUp(): expecting a LW running on port " +
              LW_PORT);
        try {
            Socket sock = new Socket("localhost", LW_PORT);
        }
        catch (Exception e) {
            System.out.println("BrowseHostTester.setUp(): could not bind!");
        }
    }

    
    public void testNormalBrowse() {
        try {
        Socket socket = new Socket("localhost", LW_PORT);
        String str = null;
        OutputStream oStream = socket.getOutputStream();
        final String LF = "\r\n";

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + "localhost" + 
              ":-1" + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: BrowseHostTester" + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connection: close" + LF;
        oStream.write(str.getBytes());
        str = "Accept: text/html," + Constants.QUERYREPLY_MIME_TYPE + LF;
        oStream.write(str.getBytes());
        str = LF;
        oStream.write(str.getBytes());
        oStream.flush();
        
        // get the results...
        InputStream in = socket.getInputStream();

        // first check the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);

        // now confirm the content-type, the encoding, etc...
        boolean readingHTTP = true;
        String currLine = null;
        while (readingHTTP) {
            currLine = br.readLine();
            if (indexOfIgnoreCase(currLine, "User-Agent") > -1)
                ; // just skip, who cares?
            else if (indexOfIgnoreCase(currLine, "HTTP") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine,"200") < 0)
                    junit.framework.Assert.assertTrue("Did not return code 200.", false);
            }
            else if (indexOfIgnoreCase(currLine, "Content-Type") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine, 
                                      Constants.QUERYREPLY_MIME_TYPE) < 0)
                    junit.framework.Assert.assertTrue("Did not return Query Replies.", false);
            }
            else if (indexOfIgnoreCase(currLine, "Content-Encoding") > -1) {
                junit.framework.Assert.assertTrue("Returned compressed data!", false);  
            }
            else if ((currLine == null) || currLine.equals("")) {
                // start processing queries...
                readingHTTP = false;
            }
        }

        // ok, everything checks out, proceed and read QRs...
        Message m = null;
        while(true) {
            try {
                m = null;
                m = Message.read(in);
            }
            catch (BadPacketException bpe) {
                junit.framework.Assert.assertTrue("Could not construct QR!", false);
            }
            catch (IOException bpe) {
                // thrown when stream is closed
            }
            if(m == null) 
                //we are finished reading the stream
                return;
            else {
                if(m instanceof QueryReply) {
                    QueryReply queryReply = (QueryReply)m;
                    // use this later for your push....
                    serventID = queryReply.getClientGUID();
                }
            }
        }        
        }
        catch (Exception e) {
            e.printStackTrace();
            junit.framework.Assert.assertTrue("Received unexpected Exception!",
                                              false);
        }
    }

    // send accept types that LW can't handle...
    public void testBadBrowse1() {
        try {
        Socket socket = new Socket("localhost", LW_PORT);
        String str = null;
        OutputStream oStream = socket.getOutputStream();
        final String LF = "\r\n";

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + "localhost" + 
              ":-1" + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: BrowseHostTester" + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connection: close" + LF;
        oStream.write(str.getBytes());
        str = "Accept: text/html, crap/more-crap" + LF;
        oStream.write(str.getBytes());
        str = LF;
        oStream.write(str.getBytes());
        oStream.flush();
        
        // get the results...
        InputStream in = socket.getInputStream();

        // first check the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);

        // now confirm the content-type, the encoding, etc...
        boolean readingHTTP = true;
        String currLine = null;
        while (readingHTTP) {
            currLine = br.readLine();
            if ((currLine == null) || currLine.equals("")) {
                // start processing queries...
                readingHTTP = false;
            }
            else if (indexOfIgnoreCase(currLine, "HTTP") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine,"406") < 0)
                    junit.framework.Assert.assertTrue("(1) Did not return code 406.", false);
            }
        }
        }
        catch (Exception e) {
            e.printStackTrace();
            junit.framework.Assert.assertTrue("Received unexpected Exception!",
                                              false);
        }
    }


    // don't send any accept types!
    public void testBadBrowse2() {
        try {
        Socket socket = new Socket("localhost", LW_PORT);
        String str = null;
        OutputStream oStream = socket.getOutputStream();
        final String LF = "\r\n";

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + "localhost" + 
              ":-1" + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: BrowseHostTester" + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connection: close" + LF;
        oStream.write(str.getBytes());
        str = LF;
        oStream.write(str.getBytes());
        oStream.flush();
        
        // get the results...
        InputStream in = socket.getInputStream();

        // first check the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);

        // now confirm the content-type, the encoding, etc...
        boolean readingHTTP = true;
        String currLine = null;
        while (readingHTTP) {
            currLine = br.readLine();
            if ((currLine == null) || currLine.equals("")) {
                // start processing queries...
                readingHTTP = false;
            }
            else if (indexOfIgnoreCase(currLine, "HTTP") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine,"406") < 0)
                    junit.framework.Assert.assertTrue("(2) Did not return code 406.", false);
            }
        }
        }
        catch (Exception e) {
            e.printStackTrace();
            junit.framework.Assert.assertTrue("Received unexpected Exception!",
                                              false);
        }
    }



    // test to make sure a PushRequest works...
    public void testPushRequest() {
        final String LF = "\r\n";
        try {
            final int callbackPort = 30000;
            // set up socket to listen for giv on...
            Connection c=new Connection("localhost", 6000);
            c.initialize();
            QueryRequest query=new QueryRequest((byte)5, 0, "mp3");
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
            c.send(push);
            c.flush();
            
            ServerSocket server = new ServerSocket(callbackPort);    
            Socket socket = server.accept();

            // make sure you get a GIV
            InputStream iStream = socket.getInputStream();
            ByteReader br = new ByteReader(iStream);
            String giv = br.readLine();
            if (indexOfIgnoreCase(giv, "giv") < 0)
                junit.framework.Assert.assertTrue("Did not get a GIV!!", false);

            String str = null;
            OutputStream oStream = socket.getOutputStream();
            
            // ask for the browse results..
            str = "GET / HTTP/1.1" + LF;
            oStream.write(str.getBytes());
            str = "Host: " + "localhost" + 
            ":-1" + LF;
            oStream.write(str.getBytes());
            str = "User-Agent: BrowseHostTester" + LF;
            oStream.write(str.getBytes());
            str = "Content-Length: 0" + LF;
            oStream.write(str.getBytes());
            str = "Connection: close" + LF;
            oStream.write(str.getBytes());
            str = "Accept: text/html, crap/more-crap" + LF;
            oStream.write(str.getBytes());
            str = LF;
            oStream.write(str.getBytes());
            oStream.flush();
            
            // get the results...
            InputStream in = socket.getInputStream();
            
            // first check the HTTP code, encoding, etc...
            br = new ByteReader(in);
            
            // now confirm the content-type, the encoding, etc...
            boolean readingHTTP = true;
            String currLine = null;
            while (readingHTTP) {
                currLine = br.readLine();
                if ((currLine == null) || currLine.equals("")) {
                    // start processing queries...
                    readingHTTP = false;
                }
                else if (indexOfIgnoreCase(currLine, "HTTP") > -1) {
                    // make sure it is QRs....
                    if (indexOfIgnoreCase(currLine,"406") < 0)
                        junit.framework.Assert.assertTrue("(1) Did not return code 406.", false);
                }
            }
            c.close();
            server.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            junit.framework.Assert.assertTrue("Received unexpected Exception!",
                                              false);
        }
    }




	/**
	 * a helper method to compare two strings 
	 * ignoring their case.
	 */ 
	private int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower case
		String aaa = str.toLowerCase();
		String bbb = section.toLowerCase();
		// then look for the index...
		return aaa.indexOf(bbb);
	}


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite =  new TestSuite("Browse Host Unit Test");
        suite.addTest(new TestSuite(BrowseHostTester.class));
        return suite;
    }


    private final static boolean debugOn = false;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }


}
