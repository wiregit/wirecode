package com.limegroup.gnutella.downloader;

import junit.framework.*;
import java.io.*;
import java.net.URL;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;

/** 
 * Unit tests small parts of ResumeDownloader.  See RequeryDownloadTest for
 * larger integration tests.
 * @see RequeryDownloadTest 
 */
public class ResumeDownloaderTest extends TestCase {
    static final String name="filename.txt";
    static final URN hash=TestFile.hash();
    static final int size=1111;
    static final int amountDownloaded=500;
    static final RemoteFileDesc rfd=newRFD(name, size, hash);
    static final IncompleteFileManager ifm=new IncompleteFileManager();
    static final File incompleteFile=ifm.getFile(rfd);
    static {
        VerifyingFile vf=new VerifyingFile(false);
        vf.addInterval(new Interval(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf);
		// Make sure that we don't wait for network on requery
		ManagedDownloader.NO_DELAY = true;
    }

    public ResumeDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ResumeDownloaderTest.class);
    }

    /** Returns a new ResumeDownloader with stubbed-out DownloadManager, etc. */
    private static ResumeDownloader newResumeDownloader() {
        ResumeDownloader ret=new ResumeDownloader(ifm,incompleteFile,name,size);
        ret.initialize(new DownloadManagerStub(), 
                       new FileManagerStub(), 
                       new ActivityCallbackStub());
        return ret;
    }

    private static RemoteFileDesc newRFD(String name, int size, URN hash) {
        Set urns=new HashSet(1);
        if (hash!=null)
            urns.add(hash);
        return new RemoteFileDesc("1.2.3.4", 6346, 13l,
                                  name, size,
                                  new byte[16], 56, false, 4, true, null, urns);
    }

    ////////////////////////////////////////////////////////////////////////////

    /** Tests that the progress is not 0% while requerying.
     *  This issue was reported by Sam Berlin. */
    public void testRequeryProgress() {
        ResumeDownloader downloader=newResumeDownloader();
        while (downloader.getState()!=Downloader.WAITING_FOR_RESULTS) {         
			if ( downloader.getState() != Downloader.QUEUED )
                assertEquals(Downloader.WAITING_FOR_RESULTS, 
				  downloader.getState());
            try { Thread.sleep(200); } catch (InterruptedException e) { }
		}
        assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());

        try {
            //Serialize it!
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ObjectOutputStream out=new ObjectOutputStream(baos);
            out.writeObject(downloader);
            out.flush(); out.close();
            downloader.stop();

            //Deserialize it as a different instance.  Initialize.
            ObjectInputStream in=new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
            downloader=(ResumeDownloader)in.readObject();
            in.close();
            downloader.initialize(new DownloadManagerStub(),
                                  new FileManagerStub(),
                                  new ActivityCallbackStub());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Couldn't serialize");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("No class");
        }

        //Check same state as before serialization.
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
    }

    /** Tests serialization of version 1.2 of ResumeDownloader.
     *  (LimeWire 2.7.0/2.7.1 beta.)
     */
    public void testSerialization12()
            throws IOException, ClassNotFoundException {
        tSerialization(
            "com/limegroup/gnutella/downloader/ResumeDownloader.1_2.dat", false);
    }

    /** Tests serialization of version 1.3 of ResumeDownloader.
     *  (LimeWire 2.7.3) */
    public void testSerialization13()
            throws IOException, ClassNotFoundException {
        tSerialization(
            "com/limegroup/gnutella/downloader/ResumeDownloader.1_3.dat", true);
    }
    
    /** Generic serialization testing routing. 
     *  @param file the serialized ResumeDownloader to read
     *  @param expectHash true iff there should be a hash in the downloader */
    private void tSerialization(String file, boolean expectHash) 
            throws IOException, ClassNotFoundException {
        ObjectInputStream in=new ObjectInputStream(new FileInputStream(file));
        ResumeDownloader rd=(ResumeDownloader)in.readObject();
        QueryRequest qr=rd.newRequery(0);
        assertEquals("filename.txt", qr.getQuery());
        if (expectHash) {
            assertEquals(1, qr.getQueryUrns().size());
            assertEquals(hash, qr.getQueryUrns().iterator().next());       
        } else {
            assertEquals(0, qr.getQueryUrns().size());
        }
    }


    /** Writes the ResumeDownloader.dat file generated for testSerialization.
     *  This should be run to generate a new version when ResumeDownloader
     *  changes. */
    public static void main(String args[]) {
        try {
            ResumeDownloader rd=newResumeDownloader();
            ObjectOutputStream out=new ObjectOutputStream(
                                    new FileOutputStream(
                                      "ResumeDownloader.dat"));
            out.writeObject(rd);
            out.flush();
            out.close();    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
