package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests small parts of ResumeDownloader. See RequeryDownloadTest for
 * larger integration tests.
 * 
 * @see RequeryDownloadTest
 */
public class ResumeDownloaderTest extends LimeTestCase {

    private static final String filePath = "com/limegroup/gnutella/downloader/";

    private static final String queryName = "filename";

    private static final String name = "filename.txt";

    /** A serialized version with 32 bit size field with value 11111 */
    private static final String Serialized32Bit1111 = "VTWQABLTOIADEY3PNUXGY2LNMVTXE33VOAXGO3TVORSWY3DBFZSG653ONRXWCZDFOIXFEZLTOVWWKRDPO5XGY33BMRSXFQINDZ76ODOS3QBAABCJAACV643JPJSUYAAFL5UGC43IOQABYTDDN5WS63DJNVSWO4TPOVYC6Z3OOV2GK3DMMEXVKUSOHNGAAD27NFXGG33NOBWGK5DFIZUWYZLUAAHEY2TBOZQS62LPF5DGS3DFHNGAABK7NZQW2ZLUAAJEY2TBOZQS63DBNZTS6U3UOJUW4ZZ3PBZAAM3DN5WS43DJNVSWO4TPOVYC4Z3OOV2GK3DMMEXGI33XNZWG6YLEMVZC4TLBNZQWOZLEIRXXO3TMN5QWIZLSEZ5CM5KUZ6S4SAYAAB4HEABUMNXW2LTMNFWWKZ3SN52XALTHNZ2XIZLMNRQS4ZDPO5XGY33BMRSXELSBMJZXI4TBMN2EI33XNZWG6YLEMVZIZV6IVOQERQWOAIAAA6DQONZAAELKMF3GCLTVORUWYLSIMFZWQU3FOS5EJBMVS24LONADAAAHQ4DXBQAAAAAQH5AAAAAAAAAAA6DTOIADOY3PNUXGY2LNMVTXE33VOAXGO3TVORSWY3DBFZSG653ONRXWCZDFOIXES3TDN5WXA3DFORSUM2LMMVGWC3TBM5SXFFNYJYT4LRYQXIBQAASMAADGE3DPMNVXG5AAB5GGUYLWMEXXK5DJNQXU2YLQHNGAABTIMFZWQZLTOEAH4AAKPBYHG4QACFVGC5TBFZ2XI2LMFZEGC43IJVQXABIH3LA4GFTA2EBQAASGAAFGY33BMRDGCY3UN5ZESAAJORUHEZLTNBXWYZDYOA7UAAAAAAAAADDXBAAAAAAQAAAAAALTOIAAY2TBOZQS42LPFZDGS3DFAQW2IRIOBXSP6AYAAFGAABDQMF2GQ4IAPYAAG6DQOQAEEQZ2LRRXSZ3XNFXFY2DPNVSVY6TCMFWGK5TTNN4VY3DJNVSVY2DFMFSFY3DJNVSXO2LSMVOFILJRGEYS22LOMNXW24DMMV2GKRTJNRSTGMTXAIAFY6DTOIABG2TBOZQS45LUNFWC4QLSOJQXSTDJON2HRAOSDWM4OYM5AMAACSIAARZWS6TFPBYAAAAAAB3QIAAAAAAHQ6DTOEAH4AAMH5AAAAAAAAAAY5YIAAAAAEAAAAAAA6DYONYQA7QABQ7UAAAAAAAAADDXBAAAAAAQAAAAAALUAAFGC5DUOJUWE5LUMVZXG4IAPYAAYP2AAAAAAAAABR3QQAAAAAIAAAAAAB4HQ6AAAACFO4DTOEAH4AAOOQABMVBNGEYTCLLJNZRW63LQNRSXIZKGNFWGKMZSO4BAAXDYOQAAYZTJNRSW4YLNMUXHI6DU";

    private static final int size = 1111;

    private static final int amountDownloaded = 500;

    private URN hash;

    private RemoteFileDesc rfd;

    private IncompleteFileManager ifm;

    private File incompleteFile;

    private Injector injector;

    public ResumeDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ResumeDownloaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DownloadManager.class).to(DownloadManagerStub.class);
            }            
        });
        

        hash = TestFile.hash();
        rfd = newRFD(name, size, hash);
        ifm = injector.getInstance(IncompleteFileManager.class);
        
        incompleteFile = ifm.getFile(rfd);
        VerifyingFile vf = injector.getInstance(VerifyingFileFactory.class).createVerifyingFile(size);
        vf.addInterval(Range.createRange(0, amountDownloaded - 1)); // inclusive
        ifm.addEntry(incompleteFile, vf, false);
        // make sure that we don't wait for network on re-query
        RequeryManager.NO_DELAY = true;

        DownloadManagerStub dm = (DownloadManagerStub) injector.getInstance(DownloadManager.class);
        dm.initialize();
        dm.scheduleWaitingPump();
    }
    
    /** Returns a new ResumeDownloader with stubbed-out DownloadManager, etc. */
    private ResumeDownloader newResumeDownloader() throws Exception {
        // this ResumeDownloader is started from the library, not from restart,
        // that is why the last param to init is false
        ResumeDownloader downloader = injector.getInstance(GnutellaDownloaderFactory.class).createResumeDownloader(
                incompleteFile, name, size);
        downloader.initialize();
        downloader.startDownload();
        return downloader;
    }

    private RemoteFileDesc newRFD(String name, int size, URN hash) {
        Set<URN> urns = new HashSet<URN>(1);
        if (hash != null)
            urns.add(hash);
        return new RemoteFileDesc("1.2.3.4", 6346, 13l, name, size, new byte[16], 56, false, 4,
                true, null, urns, false, false, "", null, -1, false);
    }

    // //////////////////////////////////////////////////////////////////////////

    public void testLoads32Bit() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base32
                .decode(Serialized32Bit1111)));
        ResumeDownloader loaded = (ResumeDownloader) ois.readObject();
        assertEquals(1111, loaded.getContentLength());

        // serializing this will result in different byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(loaded);
        byte[] newSerialized = baos.toByteArray();
        assertNotEquals(Serialized32Bit1111, Base32.encode(newSerialized));

        // which when deserialized will retain the proper size
        ois = new ObjectInputStream(new ByteArrayInputStream(newSerialized));
        ResumeDownloader newLoaded = (ResumeDownloader) ois.readObject();
        assertEquals(1111, newLoaded.getContentLength());
    }

    /**
     * Tests that the progress is not 0% while requerying. This issue was
     * reported by Sam Berlin.
     */
    public void testRequeryProgress() throws Exception {
        ResumeDownloader downloader = newResumeDownloader();
        while (downloader.getState() != DownloadStatus.WAITING_FOR_GNET_RESULTS) {
            if (downloader.getState() != DownloadStatus.QUEUED)
                assertEquals(DownloadStatus.GAVE_UP, downloader.getState());
            Thread.sleep(200);
        }
        
        // give the downloader time to change its state
        Thread.sleep(1000);
        assertEquals(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());

        // serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(downloader);
        out.flush();
        out.close();
        downloader.stop();

        // deserialize
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        downloader = (ResumeDownloader) in.readObject();
        downloader.initialize();
        downloader.startDownload();

        // Check same state as before serialization.
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        assertEquals(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
    }

    /**
     * Tests serialization of version 1.2 of ResumeDownloader. (LimeWire
     * 2.7.0/2.7.1 beta.)
     */
    public void testSerialization12() throws Exception {
        deserialize("ResumeDownloader.1_2.dat", false);
    }

    /**
     * Tests serialization of version 1.3 of ResumeDownloader. (LimeWire 2.7.3)
     */
    public void testSerialization13() throws Exception {
        deserialize("ResumeDownloader.1_3.dat", true);
    }

    /**
     * Generic serialization testing routing.
     * 
     * @param file the serialized ResumeDownloader to read
     * @param expectHash true iff there should be a hash in the downloader
     */
    private void deserialize(String file, boolean expectHash) throws Exception {
        ObjectInputStream in = new ConverterObjectInputStream(new FileInputStream(CommonUtils
                .getResourceFile(filePath + file)));
        try {
            ResumeDownloader rd = (ResumeDownloader) in.readObject();
            rd.initialize();
            QueryRequest qr = rd.newRequery();
            URN _hash = (URN) PrivilegedAccessor.getValue(rd, "_hash");
            if (expectHash) {
                assertEquals("unexpected hash", hash, _hash);
                // filenames were put in hash queries since everyone drops //
                assertEquals("hash query should have name", queryName, qr.getQuery());
            }

            // we never send URNs
            assertEquals("unexpected amount of urns", 0, qr.getQueryUrns().size());
            assertEquals("unexpected query name", "filename", qr.getQuery());
            assertEquals("unexpected filename", "filename.txt", rd.getSaveFile().getName());
        } finally {
            in.close();
        }
    }

//    /**
//     * Writes the ResumeDownloader.dat file generated for testSerialization.
//     * This should be run to generate a new version when ResumeDownloader
//     * changes.
//     */
//    public static void main(String args[]) {
//        try {
//            ResumeDownloader rd = newResumeDownloader();
//            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(CommonUtils
//                    .getResourceFile(filePath + "ResumeDownloader.dat")));
//            out.writeObject(rd);
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
