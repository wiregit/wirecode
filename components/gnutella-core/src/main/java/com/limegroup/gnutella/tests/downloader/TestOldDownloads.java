package com.limegroup.gnutella.tests.downloader;

import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tests.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.sun.java.util.collections.*;


/**
 * Tests backwards compatibility with old downloads.dat files.
 */
public class TestOldDownloads {
    public static void main(String arg[]) {
        System.out.println(
            "Please make sure you are in the com/.../tests/downloader directory");

        test("downloads_233.dat");
        //test("downloads_224.dat");  //Has XML serialization problem
        //test("downloads_202.dat");  //Has XML serialization problem
    }

    private static void test(String file) {
        System.out.println("-Trying to read downloads.dat from \""+file+"\"");

        //Build part of backend 
        SettingsManager.instance().setConnectOnStartup(false);
        SettingsManager.instance().setMaxSimDownload(0);  //queue everything
        TestActivityCallback callback=new TestActivityCallback();
        DownloadManager dm=new DownloadManager();
        RouterService rs=new RouterService(null, null, null, null);
        dm.initialize(callback, new MessageRouterStub(), 
                      null, new FileManagerStub());
        Assert.that(dm.readSnapshot(new File(file))==true);
        Assert.that(callback.downloaders.size()==1);
        ManagedDownloader md=(ManagedDownloader)callback.downloaders.get(0);
        Assert.that(md.getFileName().equals("Test1.mp3"), md.getFileName());
        Assert.that(md.getContentLength()==1922612);
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
