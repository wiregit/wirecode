package com.limegroup.gnutella.downloader;

import java.net.Socket;
import java.util.Arrays;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;
import junit.framework.Test;


public class PushDownloadManagerTest extends LimeTestCase {
    private PushDownloadManager pushDownloadManager;
    private PushedSocketHandlerStub browser;
    private PushedSocketHandlerStub downloader;

    public PushDownloadManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushDownloadManagerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DownloaderStub.class).asEagerSingleton();
                bind(BrowserStub.class).asEagerSingleton();
            }
        });
        pushDownloadManager = injector.getInstance(PushDownloadManager.class);
        browser = injector.getInstance(DownloaderStub.class);
        downloader = injector.getInstance(BrowserStub.class);
    }

    public void testHandleGIVDownload() {
        byte [] clientGUIDBytes = GUID.makeGuid();
        downloader.setClientGUID(clientGUIDBytes);
        Socket socket = new Socket();
        assertFalse(socket.isClosed());
        PushDownloadManager.GIVLine givLine = new PushDownloadManager.GIVLine("foo", 1, clientGUIDBytes);
        pushDownloadManager.handleGIV(socket, givLine);
        assertTrue(downloader.accepted());
        assertFalse(browser.accepted());
        assertFalse(socket.isClosed());
    }

    public void testHandleGIVBrowse() {
        byte [] clientGUIDBytes = GUID.makeGuid();
        browser.setClientGUID(clientGUIDBytes);
        Socket socket = new Socket();
        assertFalse(socket.isClosed());
        PushDownloadManager.GIVLine givLine = new PushDownloadManager.GIVLine("foo", 1, clientGUIDBytes);
        pushDownloadManager.handleGIV(socket, givLine);
        assertFalse(downloader.accepted());
        assertTrue(browser.accepted());
        assertFalse(socket.isClosed());
    }

    public void testHandleGIVReject() {
        byte [] clientGUIDBytes = GUID.makeGuid();
        Socket socket = new Socket();
        assertFalse(socket.isClosed());
        PushDownloadManager.GIVLine givLine = new PushDownloadManager.GIVLine("foo", 1, clientGUIDBytes);
        pushDownloadManager.handleGIV(socket, givLine);
        assertFalse(downloader.accepted());
        assertFalse(browser.accepted());
        assertTrue(socket.isClosed());
    }

    public static class PushedSocketHandlerStub implements PushedSocketHandler {
        byte [] clientGUID;
        boolean acceptedIncomingConnection = false;
        
        @Inject
        public PushedSocketHandlerStub() {}

        @Inject
        public void register(PushedSocketHandlerRegistry registry) {
            registry.register(this);
        }

        boolean accepted() {
            return acceptedIncomingConnection;
        }

        void setClientGUID(byte [] clientGUID) {
            this.clientGUID = clientGUID;
        }

        public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket) {
            if(Arrays.equals(this.clientGUID, clientGUID)) {
                acceptedIncomingConnection = true;
                return true;
            } else {
                return false;
            }
        }
    }
    
    @Singleton
    public static class DownloaderStub extends PushedSocketHandlerStub{}
    @Singleton
    public static class BrowserStub extends PushedSocketHandlerStub{}
}
