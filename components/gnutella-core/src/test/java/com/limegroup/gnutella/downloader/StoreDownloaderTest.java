package com.limegroup.gnutella.downloader;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.io.LocalSocketAddressService;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 *  Tests downloading from the store site
 */
public class StoreDownloaderTest extends LimeTestCase{

    private DownloadManagerStub downloadManager;
    private Injector injector; 
    
    public StoreDownloaderTest(String name) {
        super(name);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(StoreDownloaderTest.class);
    }
    
    public void setUp() throws Exception {
        doSetUp();
    }
    
    private void doSetUp(Module... modules) throws Exception {
        List<Module> allModules = new LinkedList<Module>();
        allModules.add(new AbstractModule() {
           @Override
            protected void configure() {
               bind(ConnectionManager.class).to(ConnectionManagerStub.class);
               bind(DownloadManager.class).to(DownloadManagerStub.class);
               bind(MessageRouter.class).to(MessageRouterStub.class);
               bind(FileManager.class).to(FileManagerStub.class);
               bind(NetworkManager.class).to(NetworkManagerStub.class);
            } 
        });
        allModules.addAll(Arrays.asList(modules));
        injector = LimeTestUtils.createInjector(allModules.toArray(new Module[0]));
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
        
        LocalSocketAddressProviderStub localSocketAddressProvider = new LocalSocketAddressProviderStub();
        localSocketAddressProvider.setLocalAddressPrivate(false);
        LocalSocketAddressService.setSocketAddressProvider(localSocketAddressProvider);
        
        downloadManager = (DownloadManagerStub)injector.getInstance(DownloadManager.class);       
        downloadManager.initialize();
        RequeryManager.NO_DELAY = false;
    }
    
    /**
     * Tests optimized overrides that return immediately since they're never used
     */
    public void testEmptyOverrides() throws Exception{
        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http:\\test.com");
        RemoteFileDesc rfd = StoreDownloader.createRemoteFileDesc(url, "test.txt", urn, 10L);
        
        //create a valid download
        StoreDownloader downloader = (StoreDownloader) downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
        
        assertNull(downloader.newRequery(9) );
        
        assertFalse(downloader.allowAddition(null));
        
        assertFalse(downloader.canSendRequeryNow());
        
        assertNull(downloader.getChatEnabledHost());
        
        assertFalse(downloader.hasChatEnabledHost());
        
        assertFalse(downloader.hasBrowseEnabledHost());
        
        assertEquals(0, downloader.getNumberOfAlternateLocations());
        
        assertEquals(0, downloader.getNumberOfInvalidAlternateLocations());
    }

    
    /**
     *  Tests creating a RemoteFileDesc from a url
     */
    public void testRFDCreation() throws Exception {
        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http:\\test.com");
        
        // test invalid rfd
        assertNull( StoreDownloader.createRemoteFileDesc(null, "", null, -1) );
        
        RemoteFileDesc rfd = StoreDownloader.createRemoteFileDesc(url, "test.txt", urn, 10L);
        
        assertTrue(rfd.getUrns().contains(urn));
        
        assertEquals( url, rfd.getUrl());
        
        assertEquals( "test.txt", rfd.getFileName());
        
        assertEquals( 10L, rfd.getFileSize() );
        
        
    }
    
    /**
     * Tests creating an invalid store download
     */
    public void testInvalidStoreDownloads() throws Exception {

        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http:\\test.com");
        RemoteFileDesc rfd = StoreDownloader.createRemoteFileDesc(url, "test.txt", urn, 10L);
                
        //create a valid download
        downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
        
        //test redownloading same file
        try {
            downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
            fail("File already downloading");
        }
        catch(SaveLocationException e) {
            
        }
    }
    
    /**
     * Tests creating a valid store download
     */
    public void testValidStoreDownloads() throws Exception {
        
        URN urn = UrnHelper.URNS[0];
        URL url = new URL("http:\\test.com");
        RemoteFileDesc rfd = StoreDownloader.createRemoteFileDesc(url, "test.txt", urn, 10L);
        
        
        downloadManager.downloadFromStore(rfd, false, _storeDir, "test.txt" );
    }
}
