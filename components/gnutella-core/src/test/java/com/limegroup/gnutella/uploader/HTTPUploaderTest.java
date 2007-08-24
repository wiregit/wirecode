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
import org.limewire.concurrent.Providers;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class HTTPUploaderTest extends LimeTestCase {

    private static final int PORT = 6668;

    private static MyActivityCallback cb;

    private HTTPAcceptor httpAcceptor;

    private HttpClient client;

    private URN urn1;

    private FileManagerStub fm;

    private HTTPUploadManager upMan;

    private FileDescStub fd1;

    public HTTPUploaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPUploaderTest.class);
    }

    public static void globalSetUp() throws Exception {
        cb = new MyActivityCallback();
        LimeTestUtils.setActivityCallBack(cb);

        doSettings();

        // TODO acceptor shutdown in globalTearDown()
        Acceptor acceptor = ProviderHacks.getAcceptor();
        acceptor.init();
        acceptor.start();
    }

    private static void doSettings() {
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
    }

    @Override
    protected void setUp() throws Exception {
        if (cb == null) {
            globalSetUp();
        }

        doSettings();

        cb.uploads.clear();

        Map<URN, FileDesc> urns = new HashMap<URN, FileDesc>();
        Vector<FileDesc> descs = new Vector<FileDesc>();
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");

        fd1 = new FileDescStub("abc1.txt", urn1, 0);
        urns.put(urn1, fd1);
        descs.add(fd1);

        fm = new FileManagerStub(urns, descs);

        httpAcceptor = new HTTPAcceptor();

        upMan = new HTTPUploadManager(new UploadSlotManagerImpl(), ProviderHacks.getHttpRequestHandlerFactory(), Providers.of(ProviderHacks.getContentManager()));

        httpAcceptor.start(ProviderHacks.getConnectionDispatcher());
        upMan.start(httpAcceptor, fm, cb, ProviderHacks.getMessageRouter());

        client = new HttpClient();
        HostConfiguration config = new HostConfiguration();
        config.setHost("localhost", PORT);
        client.setHostConfiguration(config);
    }

    @Override
    protected void tearDown() throws Exception {
        upMan.stop(httpAcceptor);
        httpAcceptor.stop(ProviderHacks.getConnectionDispatcher());
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
