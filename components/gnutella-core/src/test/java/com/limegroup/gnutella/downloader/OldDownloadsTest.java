package com.limegroup.gnutella.downloader;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.CommonUtils;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Tests backwards compatibility with old downloads.dat files.
 */
@SuppressWarnings("unchecked")
public class OldDownloadsTest extends com.limegroup.gnutella.util.LimeTestCase {
        
    private static final Log LOG = LogFactory.getLog(OldDownloadsTest.class);
    
    private final String filePath = "com/limegroup/gnutella/downloader/";
    
    private TestActivityCallback callback;

    private DownloadManager downloadManager;

    public OldDownloadsTest(String name) {
        super(name);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(OldDownloadsTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector(TestActivityCallback.class);
		downloadManager = injector.getInstance(DownloadManager.class);
		callback = (TestActivityCallback) injector.getInstance(ActivityCallback.class);
    }
    
    public void testREDO() {
        fail("Reimplement this test!");
    }
    
    /*
    public void testLegacy() throws Exception {
        doTest("downloads_30.dat","mpg4_golem160x90first120.avi",2777638);
        callback.clearDownloaders();
        doTest("downloads_233.dat", "Test1.mp3", 1922612);
        //doTest("downloads_224.dat");  //Has XML serialization problem
        //doTest("downloads_202.dat");  //Has XML serialization problem
    }

    private void doTest(String file,String name,int size) throws Exception {
        LOG.debug("-Trying to read downloads.dat from \""+file+"\"");

        //Build part of backend 
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        DownloadSettings.MAX_SIM_DOWNLOAD.setValue(0);
        DownloadManager dm = downloadManager;
        dm.initialize();
        assertTrue("unable to read snapshot!",
            dm.readAndInitializeSnapshot(CommonUtils.getResourceFile(filePath + file)));
        assertEquals("unexpected amount of downloaders added to gui",
             1, callback.downloaders.size());
        ManagedDownloader md=(ManagedDownloader)callback.downloaders.get(0);
        assertEquals("unexpected filename",
             name, md.getSaveFile().getName());
        assertEquals("unexpected content length!",
             size, md.getContentLength());
    } */
    
    /**
     * Records lists of downloads
     */
    @Singleton
    static class TestActivityCallback extends ActivityCallbackStub {
        List /* of Downloader */ downloaders=new LinkedList();
    
        public void addDownload(Downloader d) {
            downloaders.add(d);
        }
    
        public void removeDownload(Downloader d) {
            downloaders.remove(d);
        }
        
        public void clearDownloaders() {
            downloaders.clear();
        }
    }
}
