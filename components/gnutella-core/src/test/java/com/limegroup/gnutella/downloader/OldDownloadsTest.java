package com.limegroup.gnutella.downloader;

import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import com.limegroup.gnutella.settings.*;

/**
 * Tests backwards compatibility with old downloads.dat files.
 */
public class OldDownloadsTest extends com.limegroup.gnutella.util.BaseTestCase {
    private static final String filePath = "com/limegroup/gnutella/downloader/";

    public OldDownloadsTest(String name) {
        super(name);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(OldDownloadsTest.class);
    }

    public void testLegacy() throws Exception {
        doTest("downloads_30.dat","mpg4_golem160x90first120.avi",2777638);
        doTest("downloads_233.dat", "Test1.mp3", 1922612);
        //doTest("downloads_224.dat");  //Has XML serialization problem
        //doTest("downloads_202.dat");  //Has XML serialization problem
    }

    private void doTest(String file,String name,int size) throws Exception {
        DownloadTest.debug("-Trying to read downloads.dat from \""+file+"\"");

        //Build part of backend 
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        DownloadSettings.MAX_SIM_DOWNLOAD.setValue(0);
        TestActivityCallback callback=new TestActivityCallback();
        RouterService rs = new RouterService(callback);
        DownloadManager dm = rs.getDownloadManager();
        dm.initialize();
        assertTrue("unable to read snapshot!",
            dm.readSnapshot(CommonUtils.getResourceFile(filePath + file)));
        assertEquals("unexpected amount of downloaders added to gui",
             1, callback.downloaders.size());
        ManagedDownloader md=(ManagedDownloader)callback.downloaders.get(0);
        assertEquals("unexpected filename",
             name, md.getFileName());
        assertEquals("unexpected content length!",
             size, md.getContentLength());
    }
    
    /**
     * Records lists of downloads
     */
    class TestActivityCallback extends ActivityCallbackStub {
        List /* of Downloader */ downloaders=new LinkedList();
    
        public void addDownload(Downloader d) {
            downloaders.add(d);
        }
    
        public void removeDownload(Downloader d) {
            downloaders.remove(d);
        }
    }
}
