package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import junit.framework.Test;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadType;
import com.limegroup.gnutella.util.LimeTestCase;

public class HTTPUploadManagerTest extends LimeTestCase {

    private static final int PORT = 6668;

    private static final String CRLF = "\r\n";

    private static final String testDirName = "com/limegroup/gnutella/data";

    private HTTPUploadManager upMan;

    private FileManagerStub fm;

    private static MyActivityCallback cb;

    private URN urn1;

    private RemoteFileDesc rfd1;

    private Socket socket;

    private BufferedReader in;

    private BufferedWriter out;

    private static Acceptor acceptor;

    private HTTPAcceptor httpAcceptor;

    public HTTPUploadManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPUploadManagerTest.class);
    }

    public static void globalSetUp() throws Exception {
        cb = new MyActivityCallback();
        LimeTestUtils.setActivityCallBack(cb);

        doSettings();

        acceptor = RouterService.getAcceptor();
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

        // copy resources
        File targetFile = new File(_settingsDir, "update.xml");
        CommonUtils.copyResourceFile(testDirName + "/update.xml", targetFile);

        cb.uploads.clear();

        Map<URN, FileDesc> urns = new HashMap<URN, FileDesc>();
        Vector<FileDesc> descs = new Vector<FileDesc>();
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");

        FileDescStub descStub = new FileDescStub("abc1.txt", urn1, 0);
        urns.put(urn1, descStub);
        descs.add(descStub);
        rfd1 = new RemoteFileDesc("1.1.1.1", 1, 0, "abc1.txt",
                FileDescStub.DEFAULT_SIZE, new byte[16], 56, false, 3, false,
                null, descStub.getUrns(), false, false, "", null, -1);

        fm = new FileManagerStub(urns, descs);

        httpAcceptor = new HTTPAcceptor();

        upMan = new HTTPUploadManager(new UploadSlotManager());
        upMan.setFileManager(fm);

        httpAcceptor.start(RouterService.getConnectionDispatcher());
        upMan.start(httpAcceptor);
    }

    @Override
    protected void tearDown() throws Exception {
        close();

        upMan.stop(httpAcceptor);
        httpAcceptor.stop(RouterService.getConnectionDispatcher());
    }

    public void testAmountRead() throws Exception {
        connect("/get/0/" + rfd1.getFileName(), 200);
        assertEquals(1, cb.uploads.size());

        HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
        assertEquals(UploadType.SHARED_FILE, uploader.getUploadType());

        readBytes(500);
        Thread.sleep(500);
        assertGreaterThanOrEquals(500, uploader.amountUploaded());

        readBytes(500);
        Thread.sleep(500);
        assertGreaterThanOrEquals(1000, uploader.amountUploaded());

        close();
        assertEquals(Uploader.COMPLETE, uploader.getState());
    }

    public void testUpdateXML() throws Exception {
        connect("/update.xml", 200);
        LimeTestUtils.waitForNIO();
        assertEquals(1, cb.uploads.size());

        HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
        assertEquals(UploadType.UPDATE_FILE, uploader.getUploadType());

        readBytes(26);
        // make sure the NIO thread is finished processing and uploader has been
        // updated
        LimeTestUtils.waitForNIO();
        assertGreaterThanOrEquals(26, uploader.amountUploaded());

        close();
        assertEquals(Uploader.COMPLETE, uploader.getState());
    }

    /**
     * Sends a GET request for uri.
     * 
     * @return the returned headers
     */
    private List<String> connect(String uri, int expectedCode)
            throws IOException {
        socket = new Socket("localhost", PORT);
        socket.setSoTimeout(2000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket
                .getOutputStream()));

        out.write("GET " + uri + " HTTP/1.1" + CRLF);
        out.write(CRLF);
        out.flush();

        String response = in.readLine();
        assertNotNull("Server unexpectedly closed connection", response);
        if (!response.startsWith("HTTP/1.1")) {
            fail("Expected HTTP/1.1, got: " + response);
        }

        StringTokenizer t = new StringTokenizer(response);
        t.nextToken();
        assertEquals("Unexpected response code", "" + expectedCode, t
                .nextToken());

        // read headers
        ArrayList<String> headers = new ArrayList<String>();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() == 0) {
                return headers;
            }
            headers.add(line);
        }

        fail("Unexpected end of stream");

        // never reached
        return null;
    }

    private void readBytes(long count) throws IOException {
        for (long i = 0; i < count; i++) {
            try {
                if (in.read() == -1) {
                    fail("Unexpected end of stream after " + i + " bytes");
                }
            } catch (SocketTimeoutException e) {
                fail("Timeout while reading " + count + " bytes (read " + i
                        + " bytes)");
            }
        }
    }

    private void close() throws Exception {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
                Thread.sleep(500);
            }
        } catch (IOException e) {
        }
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
