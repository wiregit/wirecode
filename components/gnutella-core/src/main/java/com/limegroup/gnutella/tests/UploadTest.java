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
            String output;

            output=downloadPush(file, null);
            test("Push download",
                 output.equals("abcdefghijklmnopqrstuvwxyz"),
                 output);
            
            output=downloadPush(encodedFile, null);
            test("Push download, encoded file name",
                 output.equals("abcdefghijklmnopqrstuvwxyz"),
                 output);

            output=downloadPush(file, "Range: bytes=2-5");
            test("Push download, middle range, inclusive",
                 output.equals("cdef"),
                 output);

            output=download(file, null);
            test("No range header",
                 output.equals("abcdefghijklmnopqrstuvwxyz"),
                 output);

            output=download(file, "Range: bytes=2-");
            test("Standard range header",
                 output.equals("cdefghijklmnopqrstuvwxyz"),
                 output);

            output=download(file, "Range: bytes 2-");
            test("Range missing \"=\".  (Not legal HTTP, but common.)",
                 output.equals("cdefghijklmnopqrstuvwxyz"),
                 output);

            output=download(file, "Range: bytes=2-5");
            test("Middle range, inclusive",
                 output.equals("cdef"),
                 output);
        
            output=download(file, "Range:bytes 2-");
            test("No space after \":\".  (Legal HTTP.)",
                 output.equals("cdefghijklmnopqrstuvwxyz"),
                 output);

            output=download(file, "Range: bytes=-5");
            test("Last bytes of file",
                 output.equals("vwxyz"),
                 output);

            output=download(file, "Range:   bytes=  2  -  5 ");
            test("Lots of extra space",
                 output.equals("cdef"),
                 output);

            Assert.that(URLDecoder.decode(encodedFile).equals(file),
                        "Unexpected: "+URLDecoder.decode(encodedFile));
            output=download(encodedFile, null);
            test("URL encoded",
                 output.equals("abcdefghijklmnopqrstuvwxyz"),
                 output);            
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
    private static String download(String file, String header) 
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
        return ret;
    }

    private static String downloadPush(String file, String header) 
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
        String ret=downloadInternal(file, header, out, in);

        //Cleanup
        c.close();
        s.close();
        ss.close();        
        return ret;
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
    

    private static void test(String testName, boolean passed, String output) {
        System.out.println((passed?"passed":"FAILED")+" : "+testName);
        if (!passed)
            System.out.println("    unexpected output: \""+output+"\"");
    }
}
