package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Test;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.SimpleFileManager;

/**
 * Tests RequeryDownloader serialization.
 * @see RequeryDownloadTest
 */
public class RequeryDownloaderTest extends com.limegroup.gnutella.util.BaseTestCase {

    public RequeryDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RequeryDownloaderTest.class);
    }

	public void testLegacy() throws Exception {
        //Test serialization.
        AutoDownloadDetails details=new AutoDownloadDetails(
            "test", "", new byte[16], new MediaType("", "", new String[0]));
        IncompleteFileManager ifm=new IncompleteFileManager();
        VerifyingFile vf = new VerifyingFile(true);
        vf.addInterval(new Interval(10,20));
        ifm.addEntry(new File("T-10-test.txt"), vf);
        RequeryDownloader downloader=new RequeryDownloader(ifm, details, null);
        downloader.initialize(new DownloadManager(), 
                              new SimpleFileManager(),
                              new ActivityCallbackStub(), false);
        File tmp=File.createTempFile("RequeryDownloader_test", "dat");
        ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(tmp));
        out.writeObject(downloader);
        out.close();
        ObjectInputStream in=new ObjectInputStream(new FileInputStream(tmp));
        RequeryDownloader downloader2=(RequeryDownloader)in.readObject();
        in.close();
        assertEquals(downloader.hasFile(), downloader2.hasFile());//weak test
        tmp.delete();
    }
}
