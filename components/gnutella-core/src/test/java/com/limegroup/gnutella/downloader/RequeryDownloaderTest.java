package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import java.io.*;
import junit.framework.*;

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
        RequeryDownloader downloader=new RequeryDownloader(ifm, details);
        downloader.initialize(new DownloadManager(), 
                              new FileManager(),
                              new ActivityCallbackStub());
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
