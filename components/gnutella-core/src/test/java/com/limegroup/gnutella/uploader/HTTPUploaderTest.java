package com.limegroup.gnutella.uploader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import junit.framework.Test;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

//ITEST
public class HTTPUploaderTest extends LimeTestCase {

    private static final int PORT = 6668;

    private static MyActivityCallback cb;

    private HTTPAcceptor httpAcceptor;

    private HttpClient client;

    private URN urn1;

    private FileManagerStub fm;

    private HTTPUploadManager upMan;

    private FileDescStub fd1;

    private Acceptor acceptor;

    public HTTPUploaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPUploaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);

        Map<URN, FileDesc> urns = new HashMap<URN, FileDesc>();
        Vector<FileDesc> descs = new Vector<FileDesc>();
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");

        fd1 = new FileDescStub("abc1.txt", urn1, 0);
        urns.put(urn1, fd1);
        descs.add(fd1);

        fm = new FileManagerStub(urns, descs);

        Injector injector = LimeTestUtils.createInjector(MyActivityCallback.class, new AbstractModule() {
            @Override
            protected void configure() {
                bind(FileManager.class).toInstance(fm);
            } 
        });
        
        cb = (MyActivityCallback) injector.getInstance(ActivityCallback.class);

        acceptor = injector.getInstance(Acceptor.class);
        httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        upMan = injector.getInstance(HTTPUploadManager.class);

        acceptor.setListeningPort(PORT);
        acceptor.start();
        
        httpAcceptor.start();
        upMan.start();

        client = new HttpClient();
        HostConfiguration config = new HostConfiguration();
        config.setHost("localhost", PORT);
        client.setHostConfiguration(config);
    }

    @Override
    protected void tearDown() throws Exception {
        upMan.stop();
        httpAcceptor.stop();
        
        acceptor.setListeningPort(0);
        acceptor.shutdown();
        
        LimeTestUtils.waitForNIO();
    }

    public void testChatAndBrowseEnabled() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + urn1);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertFalse(uploader.isChatEnabled());
            assertFalse(uploader.isBrowseHostEnabled());
            assertEquals("127.0.0.1", uploader.getHost());
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod("/uri-res/N2R?" + urn1);
        method.addRequestHeader("X-Features", "chat/0.1");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertFalse(uploader.isChatEnabled());
            assertFalse(uploader.isBrowseHostEnabled());
            assertEquals("127.0.0.1", uploader.getHost());

        } finally {
            method.releaseConnection();
        }

        method = new GetMethod("/uri-res/N2R?" + urn1);
        method.addRequestHeader("X-Node", "123.123.123.123:456");
        method.addRequestHeader("X-Features", "chat/0.1");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertTrue(uploader.isChatEnabled());
            assertFalse(uploader.isBrowseHostEnabled());
            assertEquals(456, uploader.getGnutellaPort());
            assertEquals("123.123.123.123", uploader.getHost());
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod("/uri-res/N2R?" + urn1);
        method.addRequestHeader("Chat", "123.123.123.123:456");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertTrue(uploader.isChatEnabled());
            assertTrue(uploader.isBrowseHostEnabled());
            assertEquals(456, uploader.getGnutellaPort());
            assertEquals("123.123.123.123", uploader.getHost());
        } finally {
            method.releaseConnection();
        }
    }

    public void testAmountRead() throws Exception {
        HTTPUploader uploader;
        GetMethod method = new GetMethod("/get/0/" + fd1.getFileName());
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals(1, cb.uploads.size());

            uploader = (HTTPUploader) cb.uploads.get(0);
            assertEquals(UploadType.SHARED_FILE, uploader.getUploadType());

            InputStream in = method.getResponseBodyAsStream();

            LimeTestUtils.readBytes(in, 500);
            Thread.sleep(500);
            assertGreaterThanOrEquals(500, uploader.amountUploaded());

            LimeTestUtils.readBytes(in, 500);
            Thread.sleep(500);
            assertGreaterThanOrEquals(1000, uploader.amountUploaded());
        } finally {
            method.releaseConnection();
        }
        LimeTestUtils.waitForNIO();
        assertEquals(UploadStatus.COMPLETE, uploader.getState());
    }

    @Singleton
    private static class MyActivityCallback extends ActivityCallbackStub {

        List<Uploader> uploads = new ArrayList<Uploader>();

        @Override
        public void addUpload(Uploader u) {
            uploads.add(u);
        }

        @Override
        public void removeUpload(Uploader u) {
            boolean removed = uploads.remove(u);
            assertTrue("Upload has been added before", removed);
        }

    }

}
