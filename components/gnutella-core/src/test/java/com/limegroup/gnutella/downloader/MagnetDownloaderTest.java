package com.limegroup.gnutella.downloader;

import junit.framework.*;
import java.io.*;
import java.net.URL;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;

/** 
 * Unit tests small parts of MagnetDownloader.  See RequeryDownloadTest for
 * larger integration tests.
 * @see RequeryDownloadTest 
 */
public class MagnetDownloaderTest extends TestCase {
    static final URN hash=TestFile.hash();

    public MagnetDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MagnetDownloaderTest.class);
    }

    /** Returns a new MagnetDownloader with stubbed-out DownloadManager, etc. */
    private static MagnetDownloader newMagnetDownloader(
            URN hash, String textQuery, String filename, String defaultURL) {
        return new MagnetDownloader(
             new DownloadManagerStub(), 
             new FileManagerStub(), 
             new IncompleteFileManager(), 
             new ActivityCallbackStub(),
             hash, textQuery, filename, defaultURL);
    }

    private static RemoteFileDesc newRFD(String name) {
        return newRFD(name, null);
    }

    private static RemoteFileDesc newRFD(String name, URN hash) {
        Set urns=null;
        if (hash!=null) {
            urns=new HashSet(1);
            urns.add(hash);          
        }        
        return new RemoteFileDesc("1.2.3.4", 6346, 13l,
                                  name, 1024,
                                  new byte[16], 56, false, 4, true, null, urns);
    }

    ////////////////////////////////////////////////////////////////////////////

    public void testFilename() {
        try {
            final String prefix=MagnetDownloader.DOWNLOAD_PREFIX;
            assertEquals("file.txt", MagnetDownloader.filename("file.txt", new URL(
                         "http://mit.edu/ignore.txt")));
            assertEquals("file.txt", MagnetDownloader.filename(null, new URL(
                         "http://mit.edu/file.txt")));
            assertEquals("file.txt", MagnetDownloader.filename(null, new URL(
                         "http://mit.edu:6346/path/to/file.txt")));
            assertEquals("file", MagnetDownloader.filename(null, new URL(
                         "http://mit.edu/file")));
            assertEquals("file", MagnetDownloader.filename(null, new URL(
                         "http://mit.edu/path/to/file")));
            assertEquals(prefix+"mit.edu", 
                         MagnetDownloader.filename(null, new URL(
                             "http://mit.edu/path/")));
            assertEquals(prefix+"mit.edu", 
                         MagnetDownloader.filename(null, new URL(
                             "http://mit.edu:6346")));
            assertEquals(prefix+"mit.edu", 
                         MagnetDownloader.filename(null, new URL(
                             "http://mit.edu/")));
        } catch (IOException e) {
            fail("Couldn't make URL");
        }
    }

    public void testSerialization() throws IOException, ClassNotFoundException {
        ObjectInputStream in=new ObjectInputStream(new FileInputStream(
            "com/limegroup/gnutella/downloader/MagnetDownloader.1_1.dat"));
        MagnetDownloader md=(MagnetDownloader)in.readObject();
        assertEquals("name.txt", md.getFileName());
        QueryRequest qr=md.newRequery(0);
        assertEquals("text query", qr.getQuery());
        assertEquals(1, qr.getQueryUrns().size());
        assertEquals(hash, qr.getQueryUrns().iterator().next());       
    }

    public void testNoHash() {
        MagnetDownloader md=newMagnetDownloader(null, "text query", null, null);
        QueryRequest qr=md.newRequery(0);
        assertEquals("text query", qr.getQuery());
        assertEquals(0, qr.getQueryUrns().size());
        assertTrue(md.allowAddition(newRFD("text extra query")));
        assertTrue(md.allowAddition(newRFD("text extra query", hash)));
        assertTrue(! md.allowAddition(newRFD("text extra quary")));
        assertTrue(! md.allowAddition(newRFD("text extra quary", hash)));
    }

    public void testOnlyHash() {
        MagnetDownloader md=newMagnetDownloader(hash, null, null, null);
        QueryRequest qr=md.newRequery(0);
        assertEquals("", qr.getQuery());
        assertEquals(1, qr.getQueryUrns().size());
        assertEquals(hash, qr.getQueryUrns().iterator().next());    
        assertTrue(! md.allowAddition(newRFD("text query")));
        assertTrue(md.allowAddition(newRFD("text query", hash)));
        assertTrue(md.allowAddition(newRFD("", hash)));
        URN hash2=null;
        try {
            hash2=URN.createSHA1Urn("urn:sha1:TSUIEFABDMVUDXZMJEBQWNI6RYHTNIJV");
        } catch (IOException e) {
            fail("Legal hash rejected");
        }
        assertTrue(! md.allowAddition(newRFD("", hash2)));
    }

    /** Writes the MagnetDownloader.dat file generated for testSerialization.
     *  This should be run to generate a new version when MagnetDownloader
     *  changes. */
    public static void main(String args[]) {
        try {
            MagnetDownloader md=newMagnetDownloader(
                hash, "text query", "name.txt", "http://limewire.com");
            ObjectOutputStream out=new ObjectOutputStream(
                                    new FileOutputStream(
                                      "MagnetDownloader.dat"));
            out.writeObject(md);
            out.flush();
            out.close();    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
