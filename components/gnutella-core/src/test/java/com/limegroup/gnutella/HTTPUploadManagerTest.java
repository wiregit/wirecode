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

        httpAcceptor.start(RouterService.getConnectionDispatcher());
        upMan.start(httpAcceptor, fm, cb, ProviderHacks.getNewStandardMessageRouter());
    }

    @Override
    protected void tearDown() throws Exception {
        upMan.stop(httpAcceptor);
        httpAcceptor.stop(RouterService.getConnectionDispatcher());
    }

    public void testIsConnectedTo() throws Exception {
        assertFalse(upMan.isConnectedTo(InetAddress.getLocalHost()));
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
