package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.net.*;

/**
 * An incomplete, lightweight implementation of an outgoing Gnutella messaging
 * connection.  Provided for testing purposes only.<p>
 *
 * All methods use socket timeouts.  All mutators flush data to the socket.
 * Most methods can throw IOException for one of two reasons: the socket was
 * closed or there was a protocol error.  Many methods refer to end-of-line
 * (EOL), this can either be "\r\n" for Gnutella 0.6 or "\n" for Gnutella
 * 0.4.<p>
 *
 * This class does not deal with character encoding carefully and hence may not
 * work right on non-English versions of Java.  
 */
class TestConnection {
    private InputStream in;
    private OutputStream out;
    /** Include carriage-return ("\r") in EOL? */
    private boolean includeCR;
    /** The time to wait for messages, in milliseconds. */
    public static final int TIMEOUT=1000;

    /** 
     * Creates a new outgoing connection to host:port, blocking until the
     * connection is established.  Does NOT attempt to handshake the connection.
     * Throws IOException if the connection attempt failed.  If includeCR is
     * true, end-of-line (EOL) is set to "\r\n"; otherwise EOL is set to "\n". 
     */
    public TestConnection(String host, int port, boolean includeCR) 
            throws IOException {
        Socket s=new Socket(host, port);
        s.setSoTimeout(TIMEOUT);
        in=s.getInputStream();
        out=s.getOutputStream();
        this.includeCR=includeCR;
    }

    /** 
     * Writes s+EOL to this. Throws IOException if the connection closed.
     */ 
    public void sendLine(String s) throws IOException {
        out.write(s.getBytes());
        String eol=includeCR ? "\r\n" : "\n";
        out.write(eol.getBytes());
        out.flush();
    }

    /*
     * Attempts to read s+EOL from this.  Throws IOException if a string other
     * than s+EOL was read, or there was some other IO problem.  
     */
    public void expectLine(String s) throws IOException {
        for (int i=0; i<s.length(); i++) {
            int c=in.read();
            if (s.charAt(i)!=(char)c)
                throw new IOException("Unexpected "+i
                                      +"'th character '"+c+"' of \""+s+"\"");
        }
        if (includeCR && in.read()!='\r')
            throw new IOException("Missing carriage-return");
        if (in.read()!='\n')
            throw new IOException("Missing end-of-line");
    }
    
    /*
     * If includeCR is false, behavior is undefined.  Otherwise, reads
     * (blocking) all headers from this.  "All headers" headers is defined to be
     * a sequence of zero or more headers followed by EOL, where a "header" is a
     * sequence of one or more non-EOL characters followed by EOL.  Throws
     * IOException if the headers couldn't be read in a reasonable amount of
     * time or there other IO problem.  
     */
    public void expectHeaders() throws IOException {
        if (!includeCR)
            throw new IOException("Precondition violated");

        for (int i=0 ; ; i++) {
            //Read a header character.  Take special action if \r\n or EOL.
            int c=in.read();
            if (c==-1)
                throw new IOException("Unexpected EOF");
            else if ((char)c=='\n')
                throw new IOException("Missing \\r before \\n");           
            else if ((char)c=='\r') {
                if ((char)in.read()!='\n')
                    throw new IOException("Missing \\n after \\r");
                if (i==0) 
                    //Success! Got EOL with no headers
                    return;  

                //Got a header abc\r\n.  
                //Now we expect EOL or another header.
                c=in.read();
                if (c==-1)
                    throw new IOException("Unexpected EOF (2)");
                else if ((char)c=='\n')
                    throw new IOException("Missing \\r before \\n (2)");
                else if ((char)c=='\r') {
                    if ((char)in.read()=='\n')
                        //Success!  Got EOL.
                        return;    
                    else
                        throw new IOException("Missing \\n after \\r (2)");
                }
            }
        }
        //Should never get here.
    }

    /**
     * Tests that c is in sync by sending a ping and attempting to read binary
     * messages (not necessarily PingReply's) in response.  Throws IOException
     * if a bad packet was read, e.g., because the connection is out of sync, no
     * packet could be read in a reasonable amount of time, or if the connection
     * was closed.  
     */
    public void testBinary() throws IOException {
        PingRequest pr=new PingRequest((byte)1);
        pr.write(out);
        out.flush();

        try {
            Message ret=Message.read(in);
            if (ret==null)
                throw new IOException();       
        } catch (BadPacketException e) {
            throw new IOException("Got bad packet.  Out of sync.");
        }
    }
    
    /** Closes this. */
    public void close() {
        try { in.close(); } catch (IOException e) { }
        try { out.close(); } catch (IOException e) { }
    }

//      /** Unit test (for readHeaders) */
//      public static void main(String args[]) {
//          TestConnection c;
//          try {
//              c=new TestConnection("\r\nx");
//              c.expectHeaders();
//              Assert.that((char)c.in.read()=='x');

//              c=new TestConnection("abc\r\n\r\nx");
//              c.expectHeaders();
//              Assert.that((char)c.in.read()=='x');

//              c=new TestConnection("abc\r\nd\r\n\r\nx");
//              c.expectHeaders();
//              Assert.that((char)c.in.read()=='x');
//          } catch (IOException e) {
//              e.printStackTrace();
//              Assert.that(false);
//          }

//          try {
//              c=new TestConnection("a\r\nx");
//              c.expectHeaders();
//              Assert.that(false);
//          } catch (IOException e) {
//          }

//          try {
//              c=new TestConnection("a\nb\n\nx");
//              c.expectHeaders();
//              Assert.that(false);
//          } catch (IOException e) {
//          }

//          try {
//              c=new TestConnection("a\rb\r\rx");
//              c.expectHeaders();
//              Assert.that(false);
//          } catch (IOException e) {
//          }
//      }

//      private TestConnection(String s) {
//          this.includeCR=true;
//          in=new ByteArrayInputStream(s.getBytes());
//      }
}
