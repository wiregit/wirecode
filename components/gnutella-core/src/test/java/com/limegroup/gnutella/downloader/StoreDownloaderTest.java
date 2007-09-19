package com.limegroup.gnutella.downloader;

import java.io.File;

import junit.framework.Test;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class StoreDownloaderTest extends LimeTestCase{

    final static int PORT=6666;
    private DownloadManagerStub manager;
    private FileManager fileman;
    private ActivityCallback callback;

    private GnutellaDownloaderFactory gnutellaDownloaderFactory;
//    private MessageRouter router;
    private static ConnectionManager connectionManager;
    
    
    public StoreDownloaderTest(String name) {
        super(name);
    }

    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(StoreDownloaderTest.class);
    }
    
    public static void globalSetUp() throws Exception{
        connectionManager = new ConnectionManagerStub() {
            public boolean isConnected() {
                return true;
            }
        };
        
//        assertTrue(ProviderHacks.getConnectionServices().isConnected());
    }
    
    public void setUp() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        manager = new DownloadManagerStub();
//        fileman = new FileManagerStub();
//        callback = new ActivityCallbackStub();
//        router = new MessageRouterStub();
        manager.initialize();
    }
    
    public void testEmptyOverrides(){
        
    }

    
    public void testInvalidStoreDownloads() throws Exception {
        RemoteFileDesc rfd = null;
        manager.download(rfd, false, new File(""), "");
    }
    
    public void testValidStoreDownloads() throws Exception {
        
    }
}
