package com.limegroup.gnutella.downloader;

import junit.framework.*;
import java.io.*;
import java.net.URL;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;

/** 
 * Tests URLRemoteFileDesc serialization.
 */
public class URLRemoteFileDescTest extends TestCase {
    static URL url=null; 
    static {
        try {
            url=new URL("http://www.server.com/path/to/file.txt");
        } catch (IOException e) {
            System.err.println("Couldn't make URL!");
        }
    }

    public URLRemoteFileDescTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(URLRemoteFileDescTest.class);
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        ObjectInputStream in=new ObjectInputStream(new FileInputStream(
            "com/limegroup/gnutella/downloader/URLRemoteFileDesc.1_1.dat"));
        URLRemoteFileDesc rfd=(URLRemoteFileDesc)in.readObject();
        assertEquals("www.server2.com", rfd.getHost());
        assertEquals(6346, rfd.getPort());
        assertEquals(false, rfd.chatEnabled());
        assertEquals(true, rfd.browseHostEnabled());
        assertEquals(url, rfd.getUrl());   //important for MAGNET downloads.
    }

    /** Writes the URLRemoteFileDesc.dat file generated for testSerialization.
     *  This should be run to generate a new version when URLRemoteFileDesc
     *  changes. */
    public static void main(String args[]) {
        try {
            URLRemoteFileDesc rfd=new URLRemoteFileDesc(
                "www.server2.com",
                6346,
                0l,               //index
                "filename.txt",
                1035,             //size
                new byte[16],     //clientGUID
                56,               //speed
                false,            //chat
                3,                //quality
                true,             //browse host
                null,             //XML doc
                null,             //urns
                url);
            ObjectOutputStream out=new ObjectOutputStream(
                                    new FileOutputStream(
                                      "URLRemoteFileDesc.dat"));
            out.writeObject(rfd);
            out.flush();
            out.close();    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
