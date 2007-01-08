package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.limewire.util.CommonUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileManagerStub;

/** 
 * Unit tests small parts of ResumeDownloader.  See RequeryDownloadTest for
 * larger integration tests.
 * @see RequeryDownloadTest 
 */
@SuppressWarnings("unchecked")
public class ResumeDownloaderTest extends com.limegroup.gnutella.util.LimeTestCase {
    static final String filePath="com/limegroup/gnutella/downloader/";
    static final String queryName = "filename";
    static final String name="filename.txt";
    static final URN hash=TestFile.hash();
    static final int size=1111;
    static final int amountDownloaded=500;
    static final RemoteFileDesc rfd=newRFD(name, size, hash);
    static final IncompleteFileManager ifm=new IncompleteFileManager();
    static File incompleteFile;
    
    public static void globalSetUp() throws Exception {
        new RouterService(new ActivityCallbackStub());
        incompleteFile=ifm.getFile(rfd);
        VerifyingFile vf=new VerifyingFile(size);
        vf.addInterval(new Interval(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf);
		// Make sure that we don't wait for network on requery
		ManagedDownloader.NO_DELAY = true;
    }

    public ResumeDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ResumeDownloaderTest.class);
    }

    /** Returns a new ResumeDownloader with stubbed-out DownloadManager, etc. */
    private static ResumeDownloader newResumeDownloader() {
        // this ResumeDownloader is started from the library, not from restart,
        // that is why the last param to init is false
        ResumeDownloader ret=new ResumeDownloader(ifm,incompleteFile,name,size);
        DownloadManagerStub dm = new DownloadManagerStub();
        dm.initialize();
        dm.scheduleWaitingPump();
        ret.initialize(dm, 
                       new FileManagerStub(), 
                       new ActivityCallbackStub());
        ret.startDownload();
        return ret;
    }

    private static RemoteFileDesc newRFD(String name, int size, URN hash) {
        Set urns=new HashSet(1);
        if (hash!=null)
            urns.add(hash);
        return new RemoteFileDesc("1.2.3.4", 6346, 13l,
                                  name, size,
                                  new byte[16], 56, false, 4, true, null, urns,
                                  false, false,"",null, -1);
    }

    ////////////////////////////////////////////////////////////////////////////

    /** Tests that the progress is not 0% while requerying.
     *  This issue was reported by Sam Berlin. */
    public void testRequeryProgress() throws Exception {
        ResumeDownloader downloader=newResumeDownloader();
        while (downloader.getState()!=Downloader.WAITING_FOR_RESULTS) {         
			if ( downloader.getState() != Downloader.QUEUED )
                assertEquals(Downloader.GAVE_UP, 
				  downloader.getState());
            Thread.sleep(200);
		}
        // give the downloader time to change its state
        Thread.sleep(1000);
        assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());

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
        DownloadManager dm = new DownloadManagerStub();
        dm.initialize();
        dm.scheduleWaitingPump();
        downloader.initialize(dm,
                              new FileManagerStub(),
                              new ActivityCallbackStub());
        downloader.startDownload();

        //Check same state as before serialization.
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        assertEquals(Downloader.WAITING_FOR_USER, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
    }

    /** Tests serialization of version 1.2 of ResumeDownloader.
     *  (LimeWire 2.7.0/2.7.1 beta.)
     */
    public void testSerialization12()
            throws Exception {
        tSerialization("ResumeDownloader.1_2.dat", false);
    }

    /** Tests serialization of version 1.3 of ResumeDownloader.
     *  (LimeWire 2.7.3) */
    public void testSerialization13()
            throws Exception {
        tSerialization("ResumeDownloader.1_3.dat", true);
    }
    
    /** Generic serialization testing routing. 
     *  @param file the serialized ResumeDownloader to read
     *  @param expectHash true iff there should be a hash in the downloader */
    private void tSerialization(String file, boolean expectHash) 
            throws Exception {
        ObjectInputStream in=new ConverterObjectInputStream(
            new FileInputStream( CommonUtils.getResourceFile(filePath + file) )
        );
        ResumeDownloader rd=(ResumeDownloader)in.readObject();
        QueryRequest qr = rd.newRequery(0);
        URN _hash = (URN) PrivilegedAccessor.getValue(rd, "_hash");
        if (expectHash) {
            assertEquals("unexpected hash", hash, _hash);
            // filenames were put in hash queries since everyone drops //
            assertEquals("hash query should have name",
                queryName, qr.getQuery());
        }

        // we never send URNs
        assertEquals("unexpected amount of urns",
                     0, qr.getQueryUrns().size());
        assertEquals("unexpected query name",
                     "filename", qr.getQuery());    
        assertEquals("unexpected filename",
                     "filename.txt", rd.getSaveFile().getName());        
    }


    /** Writes the ResumeDownloader.dat file generated for testSerialization.
     *  This should be run to generate a new version when ResumeDownloader
     *  changes. */
    public static void main(String args[]) {
        try {
            ResumeDownloader rd=newResumeDownloader();
            ObjectOutputStream out=new ObjectOutputStream(
                                    new FileOutputStream(
                                      CommonUtils.getResourceFile(
                                        filePath + "ResumeDownloader.dat"
                                      )
                                    )
                                  );
            out.writeObject(rd);
            out.flush();
            out.close();    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
