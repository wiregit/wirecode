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
import com.limegroup.gnutella.uploader.FileViewUploadState;
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
        URLConnection conn = url.openConnection();
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
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        assertGreaterThan(FileListHTMLPage.htmlBegin.length() +
                          FileListHTMLPage.htmlMiddle.length() +
                          FileListHTMLPage.htmlEnd.length(),
                          conn.getContentLength());
        byte[] bytes = new byte[FileListHTMLPage.htmlBegin.length()];
        is.read(bytes);
        assertEquals(FileListHTMLPage.htmlBegin.getBytes(), bytes);
    }

    //////////////////////////////////////////////////////////////////
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

}
