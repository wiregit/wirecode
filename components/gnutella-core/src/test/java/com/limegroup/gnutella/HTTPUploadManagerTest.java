package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.uploader.UploadSlotManager;

public class HTTPUploadManagerTest extends BaseTestCase {

    private static final int PORT = 6668;

    private HTTPUploadManager upMan;

    private FileManagerStub fm;

    private static MyActivityCallback cb;

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

        // TODO acceptor shutdown in globalTearDown()
        Acceptor acceptor = RouterService.getAcceptor();
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

        fm = new FileManagerStub();

        httpAcceptor = new HTTPAcceptor();

        upMan = new HTTPUploadManager(new UploadSlotManager());
        upMan.setFileManager(fm);

        httpAcceptor.start(RouterService.getConnectionDispatcher());
        upMan.start(httpAcceptor);
    }

    @Override
    protected void tearDown() throws Exception {
        upMan.stop(httpAcceptor);
        httpAcceptor.stop(RouterService.getConnectionDispatcher());
    }

    public void testIsConnectedTo() throws Exception {
        assertFalse(upMan.isConnectedTo(InetAddress.getLocalHost()));
    }

// /**
// * Sends a GET request for <code>uri</code>.
// *
// * @return the returned headers
// */
// private List<String> connect(String uri, int expectedCode)
// throws IOException {
// socket = new Socket("localhost", PORT);
// socket.setSoTimeout(2000);
// in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
// out = new BufferedWriter(new OutputStreamWriter(socket
// .getOutputStream()));
//
// out.write("GET " + uri + " HTTP/1.1" + CRLF);
// out.write(CRLF);
// out.flush();
//
// String response = in.readLine();
// assertNotNull("Server unexpectedly closed connection", response);
// if (!response.startsWith("HTTP/1.1")) {
// fail("Expected HTTP/1.1, got: " + response);
// }
//
// StringTokenizer t = new StringTokenizer(response);
// t.nextToken();
// assertEquals("Unexpected response code", "" + expectedCode, t
// .nextToken());
//
// // read headers
// ArrayList<String> headers = new ArrayList<String>();
// String line;
// while ((line = in.readLine()) != null) {
// if (line.length() == 0) {
// return headers;
// }
// headers.add(line);
// }
//
// fail("Unexpected end of stream");
//
// // never reached
// return null;
// }

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
