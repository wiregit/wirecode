package com.limegroup.gnutella.tests;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.Assert;

/**
 * Test that a client uploads a file correctly.  Depends on a file
 * called "alphabet.txt" containing the lowercase characters a-z.
 */
public class UploadTest {
    private static String address;
    private static int port;
    private static final String file="alphabet.txt";
    private static final int index=0;

    public static void main(String args[]) {
        try {
            address=args[0];
            port=Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Syntax: UploadTest <address> <port>");
        }
        
        System.out.println(
            "Please make sure your client is listening on port "+port+"\n"
            +"of "+address+" and is sharing "+file+" in slot "+index+"\n");
        
        try {
            String output;

            output=download(null);
            test("No range header",
                 output.equals("abcdefghijklmnopqrstuvwxyz"),
                 output);

            output=download("Range: bytes=2-");
            test("Standard range header",
                 output.equals("cdefghijklmnopqrstuvwxyz"),
                 output);

            output=download("Range: bytes 2-");
            test("Range missing \"=\".  (Not legal HTTP, but common.)",
                 output.equals("cdefghijklmnopqrstuvwxyz"),
                 output);

            output=download("Range: bytes=2-5");
            test("Middle range, inclusive",
                 output.equals("cdef"),
                 output);
        
            output=download("Range:bytes 2-");
            test("No space after \":\".  (Legal HTTP.)",
                 output.equals("cdefghijklmnopqrstuvwxyz"),
                 output);

            output=download("Range: bytes=-5");
            test("Last bytes of file",
                 output.equals("vwxyz"),
                 output);

            output=download("Range:   bytes=  2  -  5 ");
            test("Lots of extra space",
                 output.equals("cdef"),
                 output);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected exception");
        }
    }

    /** 
     * Downloads file (stored in slot index) from address:port, returning the
     * content as a string. If header!=null, includes it as a request header.
     * Do not include new line or carriage return in header.  Throws IOException
     * if there is a problem, without cleaning up. 
     */
    private static String download(String header) throws IOException {
        //Unfortunately we can't use URLConnection because we need to test
        //malformed and slightly malformed headers

        //1. Write request
        Socket s = new Socket(address, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            s.getOutputStream()));
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
        in.close();
        out.close();
        s.close();
        return buf.toString();
    }
    
    private static void test(String testName, boolean passed, String output) {
        System.out.println((passed?"passed":"FAILED")+" : "+testName);
        if (!passed)
            System.out.println("    unexpected output: \""+output+"\"");
    }
}
