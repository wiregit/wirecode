package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import java.io.*;
import junit.framework.*;

public class AutoDownloaderDetailsTest extends com.limegroup.gnutella.util.BaseTestCase {

    public AutoDownloaderDetailsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AutoDownloaderDetailsTest.class);
    }

	public void testLegacy() throws Exception {
        AutoDownloadDetails add = 
        new AutoDownloadDetails("moxxiffey", null,
                                GUID.makeGuid(), MediaType.getAudioMediaType());
        String[] files = {"moxxiffey - fuedehead.mp3"};

        RemoteFileDesc[] rfds = new RemoteFileDesc[files.length];
        for (int i = 0; i < rfds.length; i++)
            rfds[i] = new RemoteFileDesc("0.0.0.0", 6346, i, files[i],
                                         i, GUID.makeGuid(),
                                         3, false, 3, false,null, null,
                                         false, false,"",0,null);

        //Test serialization by writing to disk and rereading.  All the methods
        //should still work afterwards.
        File tmp=File.createTempFile("AutoDownloadDetails_test", "dat");
        ObjectOutputStream out=
                    new ObjectOutputStream(new FileOutputStream(tmp));
        out.writeObject(add);
        out.close();
        ObjectInputStream in=
                      new ObjectInputStream(new FileInputStream(tmp));
        add=(AutoDownloadDetails)in.readObject();
        in.close();
        tmp.delete();
        
        assertTrue(add.addDownload(rfds[0]));
        add.commitDownload(rfds[0]);

        // seems like we've committed MAX_DOWNLOADS, should be expired...
        assertTrue(add.expired());
    }

}
