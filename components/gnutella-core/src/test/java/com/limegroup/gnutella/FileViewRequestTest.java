package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.uploader.*;
import com.limegroup.gnutella.html.FileListHTMLPage;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Set;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class FileViewRequestTest extends ClientSideTestCase {

    public final byte[] BAD_PASS = FileViewUploadState.BAD_PASS_REPLY;

    public FileViewRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileViewRequestTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    public void testBadFileViewRequest() throws Exception {
        
        URL url = new URL("http", "localhost", SERVER_PORT,
                          UploadManager.FV_REQ_BEGIN);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        assertEquals(BAD_PASS.length, conn.getContentLength());
        InputStream is = conn.getInputStream();
        byte[] bytes = new byte[BAD_PASS.length];
        is.read(bytes);
        assertEquals(BAD_PASS, bytes);
    }

    public void testGoodFileViewRequest() throws Exception {
        
        URL url = new URL("http", "localhost", SERVER_PORT,
                          UploadManager.FV_REQ_BEGIN + "/" +
                          UploadManager.FV_PASS);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream is = conn.getInputStream();
        final String output = FileListHTMLPage.instance().getSharedFilePage();
        assertEquals(output.length(), conn.getContentLength());
        byte[] bytes = new byte[output.length()];
        is.read(bytes);
        assertEquals(bytes, output.getBytes());
    }

    public void testHammeringNotAllowed() throws Exception {

        final String output = FileListHTMLPage.instance().getSharedFilePage();
        // open a bunch of connections
        for (int i = 0; i < 25; i++) {
            URL url = new URL("http", "localhost", SERVER_PORT,
                              UploadManager.FV_REQ_BEGIN + "/" +
                              UploadManager.FV_PASS);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            assertEquals(output.length(), conn.getContentLength());
            Thread.sleep(1*1000);
        }

        // wait a while
        Thread.sleep(25*1000);

        final byte[] error = BannedUploadState.ERROR_MESSAGE;
        // next request should be denied
        URL url = new URL("http", "localhost", SERVER_PORT,
                          UploadManager.FV_REQ_BEGIN + "/" +
                          UploadManager.FV_PASS);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        assertTrue((conn.getResponseCode() >= 400) &&
                   (conn.getResponseCode() < 500));
        
    }

    //////////////////////////////////////////////////////////////////
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

}
