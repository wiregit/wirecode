package com.limegroup.gnutella.downloader;

import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

/**
 * Tests backwards compatibility with old downloads.dat files.
 */
public class OldDownloadsTest extends com.limegroup.gnutella.util.BaseTestCase {
    private static final String filePath = "com/limegroup/gnutella/downloader/";

    public OldDownloadsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OldDownloadsTest.class);
    }



    public static void testLegacy() {
        doTest("downloads_233.dat");
        //doTest("downloads_224.dat");  //Has XML serialization problem
        //doTest("downloads_202.dat");  //Has XML serialization problem
    }

    private static void doTest(String file) {
        DownloadTest.debug("-Trying to read downloads.dat from \""+file+"\"");

        //Build part of backend 
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        //SettingsManager.instance().setConnectOnStartup(false);
        SettingsManager.instance().setMaxSimDownload(0);  //queue everything
        TestActivityCallback callback=new TestActivityCallback();
        RouterService rs = new RouterService(callback);
        assertTrue(rs.getDownloadManager().readSnapshot(
            CommonUtils.getResourceFile(filePath+file)
        ));
        assertTrue(callback.downloaders.size()==1);
        ManagedDownloader md=(ManagedDownloader)callback.downloaders.get(0);
        assertTrue(md.getFileName(),md.getFileName().equals("Test1.mp3"));
        assertTrue(md.getContentLength()==1922612);
    }
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
