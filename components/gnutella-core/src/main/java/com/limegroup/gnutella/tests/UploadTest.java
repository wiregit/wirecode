package com.limegroup.gnutella.tests;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.*;

/**
 * Test that a client uploads a file correctly.  Depends on a file
 * called "alphabet.txt" containing the lowercase characters a-z.
 */
public class UploadTest {
    private static String address;
    private static int port;
    private static String file="alphabet test file#2.txt";
    private static String encodedFile="alphabet%20test+file%232.txt";
    private static final int index=0;
    /** Our listening port for pushes. */
    private static int callbackPort=6350;


    public static void main(String args[]) {
        try {
            address=args[0];
            port=Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Syntax: UploadTest <address> <port>");
        }
        
        System.out.println(
            "Please make sure your client is listening on port "+port+"\n"
            +"of "+address+" and is sharing "+file+" in slot "+index+",\n"
            +"with at least one incoming messaging slot.  Also, nothing\n"
            +"may be listening to port "+callbackPort+"\n");
        
        try {
            boolean passed;
            
            ///////////////////push downloads with HTTP1.0///////////
            passed = downloadPush(file, null,"abcdefghijklmnopqrstuvwxyz");
            test("Push download",passed);
            
            passed=downloadPush(encodedFile, null,
                                "abcdefghijklmnopqrstuvwxyz");
            test("Push download, encoded file name",passed);

            passed=downloadPush(file, "Range: bytes=2-5","cdef");
            test("Push download, middle range, inclusive",passed);

            ///////////////////push downloads with HTTP1.1///////////////
            
            passed = downloadPush1(file, null,"abcdefghijklmnopqrstuvwxyz");
            test("Push download with HTTP1.1",passed);
            
            passed=downloadPush1(encodedFile, null,
                                "abcdefghijklmnopqrstuvwxyz");
            test("Push download, encoded file name with HTTP1.1",passed);

            passed=downloadPush1(file, "Range: bytes=2-5","cdef");
            test("Push download, middle range, inclusive with HTTP1.1",passed);
            

            //////////////normal downloads with HTTP 1.0//////////////

            passed=download(file, null,"abcdefghijklmnopqrstuvwxyz");
            test("No range header",passed);
            
            passed=download(file, "Range: bytes=2-", 
                            "cdefghijklmnopqrstuvwxyz");
            test("Standard range header",passed);


            passed=download(file, "Range: bytes 2-", 
                            "cdefghijklmnopqrstuvwxyz");
            test("Range missing \"=\".  (Not legal HTTP, but common.)",
                 passed);


            passed=download(file, "Range: bytes=2-5","cdef");
            test("Middle range, inclusive",passed);

        
            passed=download(file, "Range:bytes 2-",
                            "cdefghijklmnopqrstuvwxyz");
            test("No space after \":\".  (Legal HTTP.)",passed);

            passed=download(file, "Range: bytes=-5","vwxyz");
            test("Last bytes of file",passed);


            passed=download(file, "Range:   bytes=  2  -  5 ", "cdef");
            test("Lots of extra space",passed);


            Assert.that(URLDecoder.decode(encodedFile).equals(file),
                        "Unexpected: "+URLDecoder.decode(encodedFile));
            passed=download(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
            test("URL encoded",passed);


            ////////////normal download with HTTP 1.1////////////////

            passed=download1(file, null,"abcdefghijklmnopqrstuvwxyz");
            test("No range header with HTTP1.1",passed);

            passed=download1(file, "Range: bytes=2-", 
                            "cdefghijklmnopqrstuvwxyz");
            test("Standard range header with HTTP1.1",passed);


            passed=download1(file, "Range: bytes 2-", 
                            "cdefghijklmnopqrstuvwxyz");
            test("Range missing \"=\". (Not legal HTTP, but common.)"+
                 "with HTTP1.1", passed);


            passed=download1(file, "Range: bytes=2-5","cdef");
            test("Middle range, inclusive with HTTP1.1",passed);

        
            passed=download1(file, "Range:bytes 2-",
                            "cdefghijklmnopqrstuvwxyz");
            test("No space after \":\".  (Legal HTTP.) with HTTP1.1",passed);

            passed=download1(file, "Range: bytes=-5","vwxyz");
            test("Last bytes of file with HTTP1.1",passed);


            passed=download1(file, "Range:   bytes=  2  -  5 ", "cdef");
            test("Lots of extra space with HTTP1.1",passed);


            Assert.that(URLDecoder.decode(encodedFile).equals(file),
                        "Unexpected: "+URLDecoder.decode(encodedFile));
            passed=download1(encodedFile, null,"abcdefghijklmnopqrstuvwxyz");
            test("URL encoded with HTTP1.1",passed);
            
            //////////////////Pipelining tests with HTTP1.1////////////// 
            passed = pipelineDownloadNormal(file, null, 
                                            "abcdefghijklmnopqrstuvwxyz");
            test("piplining with normal download",passed);
            
            passed = pipelineDownloadPush(file,null, 
                                          "abcdefghijklmnopqrstuvwxyz");
            test("piplining with push download",passed);
            
        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected exception");
        } catch (BadPacketException e) {
            e.printStackTrace();
            Assert.that(false, "Bad packet");
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
        //Unfortunately we can't use URLConnection because we need to test
        //malformed and slightly malformed headers
        
        //1. Write request
        Socket s = new Socket(address, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));

        String ret=downloadInternal(file, header, out, in);
        in.close();
        out.close();
        s.close();
        return ret.equals(expResp);
    }

    private static boolean downloadPush1(String file, String header, 
                                       String expResp) 
            throws IOException, BadPacketException {
        //Establish push route
        Connection c=new Connection(address, port);
        c.initialize();
        QueryRequest query=new QueryRequest((byte)5, 0, "txt");
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

        //Download from the (incoming) TCP connection.
        String retStr=downloadInternal1(file, header, out, in,expResp.length());
        boolean ret = retStr.equals(expResp);
        retStr = "";//reset it
        
        retStr = downloadInternal1(file, header, out, in,expResp.length());
        
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
        QueryRequest query=new QueryRequest((byte)5, 0, "txt");
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

        //Download from the (incoming) TCP connection.
        String retStr=downloadInternal(file, header, out, in);

        //Cleanup
        c.close();
        s.close();
        ss.close();        
        return retStr.equals(expResp);
    }

    /** 
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.
     */
    private static String downloadInternal(String file,
                                           String header,
                                           BufferedWriter out,
                                           BufferedReader in) 
            throws IOException {
        //1. Send request
        out.write("GET /get/"+index+"/"+file+" HTTP/1.0\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("\r\n");
        out.flush();

        //2. Read (and ignore!) response code and headers.  TODO: verify.
        while (!in.readLine().equals("")) { }
        
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
    
    /** 
     * Sends a get request to out, reads the response from in, and returns the
     * content.  Doesn't close in or out.
     */
    private static String downloadInternal1(String file,
                                              String header,
                                              BufferedWriter out,
                                              BufferedReader in,
                                              int expectedSize) 
        throws IOException {
        //Assert.that(out!=null && in!=null,"socket closed my server");
        //1. Send request
        out.write("GET /get/"+index+"/"+file+" HTTP/1.1\r\n");
        if (header!=null)
            out.write(header+"\r\n");
        out.write("Connection:Keep-Alive\r\n");
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
        //System.out.println("Sumeet: about to return value " + buf);
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
        QueryRequest query=new QueryRequest((byte)5, 0, "txt");
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


    private static void test(String testName, boolean passed) {
        System.out.println((passed?"passed":"FAILED")+" : "+testName);
        if (!passed)
            System.out.println("unexpected output:");
    }
}
