package com.limegroup.gnutella.downloader.serial;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.google.inject.Injector;
import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.AbstractDownloader;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactory;
import com.limegroup.gnutella.downloader.InNetworkDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.MagnetDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PurchasedStoreDownloaderFactory;
import com.limegroup.gnutella.downloader.RequeryDownloader;
import com.limegroup.gnutella.downloader.ResumeDownloader;
import com.limegroup.gnutella.downloader.StoreDownloader;
import com.limegroup.gnutella.downloader.TestFile;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.version.DownloadInformation;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class DownloadConversionTest extends BaseTestCase {

    public DownloadConversionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DownloadConversionTest.class);
    }

    public void testConversion() throws Exception {
        File file = CommonUtils
                .getResourceFile("com/limegroup/gnutella/downloader/serial/allKindsOfDownloads.dat");
        ObjectInputStream in = new DownloadConverterObjectInputStream(new FileInputStream(file));
        Object read1 = null;
        Object read2 = null;
        try {
            read1 = in.readObject();
            read2 = in.readObject();
            try {
                Object read3 = in.readObject();
                fail("read another object: " + read3);
            } catch (EOFException expected) {
            }
        } catch (OptionalDataException ode) {
            fail("eof: " + ode.eof + ", length: " + ode.length, ode);
        }

        List<SerialRoot> serialRead = GenericsUtils.scanForList(read1, SerialRoot.class,
                ScanMode.EXCEPTION);
        SerialIncompleteFileManager serialIncompleteFileManager = (SerialIncompleteFileManager) read2;

    }

    public void donttestCreateFiles() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        GnutellaDownloaderFactory gnutellaDownloadFactory = injector
                .getInstance(GnutellaDownloaderFactory.class);
        PurchasedStoreDownloaderFactory purchasedStoreDownloaderFactory = injector
                .getInstance(PurchasedStoreDownloaderFactory.class);
        BTDownloaderFactory btDownloaderFactory = injector.getInstance(BTDownloaderFactory.class);
        SearchServices searchServices = injector.getInstance(SearchServices.class);
        DownloadReferencesFactory downloadReferencesFactory = injector
                .getInstance(DownloadReferencesFactory.class);
        IncompleteFileManager incompleteFileManager = injector
                .getInstance(IncompleteFileManager.class);
        File saveDirectory = new File("saveDirectory");
        saveDirectory.mkdirs();
        saveDirectory.deleteOnExit();

        ManagedDownloader managedDownloader;
        StoreDownloader storeDownloader;
        InNetworkDownloader inNetworkDownloader;
        ResumeDownloader resumeDownloader;
        MagnetDownloader magnetDownloader;
        RequeryDownloader requeryDownloader;
        BTDownloader btDownloader;

        byte[] guid = new GUID().bytes();
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        RemoteFileDesc rfd = new RemoteFileDesc("127.0.0.1", 1, 1, "fileA.txt", 123, guid, 1, true,
                1, false, null, UrnHelper.URN_SETS[0], false, true, "MNGD", null, -1, false);
        managedDownloader = gnutellaDownloadFactory.createManagedDownloader(
                new RemoteFileDesc[] { rfd }, incompleteFileManager, queryGuid);
        managedDownloader.setAttribute("KEY", "VALUE");

        rfd = new RemoteFileDesc("127.0.0.2", 2, 1, "fileB.txt", 123, guid, 1, true, 1, false,
                null, UrnHelper.URN_SETS[0], false, true, "STOR", null, -1, false);
        storeDownloader = purchasedStoreDownloaderFactory.createStoreDownloader(rfd,
                incompleteFileManager, saveDirectory, "name", false);

        DownloadInformation downloadInformation = new DownloadInformation() {
            public long getSize() {
                return 12356;
            }

            public String getTTRoot() {
                return TestFile.tree().getRootHash();
            }

            public String getUpdateCommand() {
                return "";
            }

            public String getUpdateFileName() {
                return "updateName";
            }

            public URN getUpdateURN() {
                return TestFile.hash();
            }
        };
        inNetworkDownloader = gnutellaDownloadFactory.createInNetworkDownloader(
                incompleteFileManager, downloadInformation, saveDirectory, 1235);

        resumeDownloader = gnutellaDownloadFactory.createResumeDownloader(incompleteFileManager,
                new File("T-123453-incompleteName"), "incompleteName", 123453);

        MagnetOptions magnet = MagnetOptions.createMagnet(new FileDetails() {
            public String getFileName() {
                return "magnetName";
            }

            public long getFileSize() {
                return 6531;
            }

            public InetSocketAddress getInetSocketAddress() {
                return new InetSocketAddress("127.0.0.3", 3);
            }

            public URN getSHA1Urn() {
                return UrnHelper.URNS[1];
            }

            public Set<URN> getUrns() {
                return UrnHelper.URN_SETS[1];
            }

            public LimeXMLDocument getXMLDocument() {
                return null;
            }

            public boolean isFirewalled() {
                return false;
            }
        });
        magnetDownloader = gnutellaDownloadFactory.createMagnetDownloader(incompleteFileManager,
                magnet, false, saveDirectory, "magnetName");

        requeryDownloader = null;
      // Requires RequeryDownloader is changed to not be deprecated
      //  requeryDownloader = new RequeryDownloader(new RemoteFileDesc[] { rfd },
      //          incompleteFileManager, queryGuid, injector.getInstance(SaveLocationManager.class));

        BTData btData = new BTData() {
            public void clearPieces() {
            }

            public String getAnnounce() {
                return "http://www.example.com/announce";
            }

            public List<BTFileData> getFiles() {
                return null;
            }

            public Set<String> getFolders() {
                return null;
            }

            public byte[] getInfoHash() {
                return UrnHelper.URNS[3].getBytes();
            }

            public Long getLength() {
                return 123L;
            }

            public String getName() {
                return "btName";
            }

            public Long getPieceLength() {
                return 1235L;
            }

            public byte[] getPieces() {
                return new byte[0];
            }

            public boolean isPrivate() {
                return true;
            }
        };
        btDownloader = btDownloaderFactory.createBTDownloader(new BTMetaInfo(btData));

        AbstractDownloader[] allDownloaders = { managedDownloader, storeDownloader,
                inNetworkDownloader, resumeDownloader, magnetDownloader, requeryDownloader,
                btDownloader };

        for (AbstractDownloader downloader : allDownloaders)
            downloader.initialize(downloadReferencesFactory.create(downloader));
        List<AbstractDownloader> downloaders = new ArrayList<AbstractDownloader>(Arrays
                .asList(allDownloaders));
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(
                "allKindsOfDownloads.dat")));
        out.writeObject(downloaders);
        out.writeObject(incompleteFileManager);
        out.flush();
        out.close();

        // Make sure we can read it!!!
        DownloadManager downloadManager = injector.getInstance(DownloadManager.class);
        List<AbstractDownloader> read = downloadManager.readSnapshot(new File(
                "allKindsOfDownloads.dat"));
        assertEquals(7, read.size());
    }

}
