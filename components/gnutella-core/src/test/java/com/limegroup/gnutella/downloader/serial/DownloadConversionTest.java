package com.limegroup.gnutella.downloader.serial;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.helpers.UrnHelper;

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
        Object read1 = in.readObject();;
        Object read2 =  in.readObject();
        try {
            Object read3 = in.readObject();
            fail("read another object: " + read3);
        } catch (EOFException expected) {
        }

        List<SerialRoot> serialRead = GenericsUtils.scanForList(read1, SerialRoot.class,
                ScanMode.EXCEPTION);
        SerialIncompleteFileManager serialIncompleteFileManager = (SerialIncompleteFileManager) read2;
        
        { // test ManagedDownloader conversion
            SerialManagedDownloader smd = (SerialManagedDownloader)serialRead.get(0);
            SerialRemoteFileDesc srfd = smd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = smd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = smd.getIncompleteFileManager();
            Map<String, Serializable> properties = smd.getProperties();
            
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(1, setSRFD.size());
            assertSame(srfd, setSRFD.iterator().next());
            assertEquals("127.0.0.1", srfd.getHost());
            assertEquals(1, srfd.getPort());
            assertEquals(1, srfd.getIndex());
            assertEquals("fileA.txt", srfd.getFilename());
            assertEquals(123, srfd.getSize());
            // CHECK GUID!!!!
            assertEquals(1, srfd.getSpeed());
            assertEquals(true, srfd.isChatEnabled());
            assertEquals(1, srfd.getQuality());
            assertEquals(false, srfd.isBrowseHostEnabled());
            assertEquals(null, srfd.getXmlDocs());
            assertEquals(UrnHelper.URN_SETS[0], srfd.getUrns());
            assertEquals(false, srfd.isReplyToMulticast());
            assertEquals(true, srfd.isFirewalled());
            assertEquals("MNGD", srfd.getVendor());
            assertNull(srfd.getPropertiesMap());
            Map<?, ?> attributes = (Map)properties.get("attributes");
            assertEquals("VALUE", attributes.get("KEY"));
            assertEquals(123L, properties.get("fileSize"));
            assertEquals("fileA.txt", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[0], properties.get("sha1Urn"));
        }
            
        {
            SerialStoreDownloader ssd = (SerialStoreDownloader)serialRead.get(1);
            SerialRemoteFileDesc srfd = ssd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = ssd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = ssd.getIncompleteFileManager();
            Map<String, Serializable> properties = ssd.getProperties();
            
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(1, setSRFD.size());
            assertSame(srfd, setSRFD.iterator().next());
            assertEquals("127.0.0.2", srfd.getHost());
            assertEquals(2, srfd.getPort());
            assertEquals(1, srfd.getIndex());
            assertEquals("fileB.txt", srfd.getFilename());
            assertEquals(123, srfd.getSize());
            // CHECK GUID!!!!
            assertEquals(1, srfd.getSpeed());
            assertEquals(true, srfd.isChatEnabled());
            assertEquals(1, srfd.getQuality());
            assertEquals(false, srfd.isBrowseHostEnabled());
            assertEquals(null, srfd.getXmlDocs());
            assertEquals(UrnHelper.URN_SETS[0], srfd.getUrns());
            assertEquals(false, srfd.isReplyToMulticast());
            assertEquals(true, srfd.isFirewalled());
            assertEquals("STOR", srfd.getVendor());
            assertNull(srfd.getPropertiesMap());
            assertEquals(123L, properties.get("fileSize"));
            assertEquals("fileB.txt", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[0], properties.get("sha1Urn"));
        }
        
        {
            SerialInNetworkDownloader sind = (SerialInNetworkDownloader)serialRead.get(2);
            SerialRemoteFileDesc srfd = sind.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = sind.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = sind.getIncompleteFileManager();
            Map<String, Serializable> properties = sind.getProperties();
 
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(0, setSRFD.size());
            assertNull(srfd);
            assertEquals("updateName", properties.get("defaultFileName"));
            assertEquals(12356, sind.getSize());
            assertEquals("7OKOEWONKM27RIQPQCCLPKECC5FKUOPRFF2NDVI", sind.getTtRoot());
            assertEquals(URN.createSHA1Urn("urn:sha1:MTSUIEFABDVUDXZMJEBQWNI6RVYHTNIJ"), sind.getUrn());
            assertEquals(1235, sind.getStartTime());
        }
        
        {
            SerialResumeDownloader srd = (SerialResumeDownloader)serialRead.get(3);
            SerialRemoteFileDesc srfd = srd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = srd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = srd.getIncompleteFileManager();
            
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(0, setSRFD.size());
            assertNull(srfd);
            assertEquals("T-123453-incompleteName", srd.getIncompleteFile().getName());
            assertEquals("incompleteName", srd.getName());
            assertEquals(123453L, srd.getSize());
        }
        
        {
            SerialMagnetDownloader smd = (SerialMagnetDownloader)serialRead.get(4);
            SerialRemoteFileDesc srfd = smd.getDefaultRFD();
            Set<SerialRemoteFileDesc> setSRFD = smd.getRemoteFileDescs();
            SerialIncompleteFileManager sifm = smd.getIncompleteFileManager();
            Map<String, Serializable> properties = smd.getProperties();
                        
            assertSame(sifm, serialIncompleteFileManager);
            assertEquals(0, setSRFD.size());
            assertNull(srfd);
            assertNull(smd.getTextQuery());
            assertNull(smd.getUrn());
            assertNull(smd.getFilename());
            assertNull(smd.getDefaultUrls());
            assertEquals("magnetName", properties.get("defaultFileName"));
            MagnetOptions mo = (MagnetOptions)properties.get("MAGNET");
            assertEquals("magnetName", mo.getDisplayName());
            assertEquals(UrnHelper.URNS[1], mo.getSHA1Urn());
            assertEquals(Collections.singletonList("http://127.0.0.3:3/uri-res/N2R?" + UrnHelper.URNS[1]), mo.getXS());
        }
        
        {
            // No need to do anything other than check these can be deserialized.
            assertInstanceof(SerialRequeryDownloader.class, serialRead.get(5));
        }
        
        {
            SerialBTDownloader sbtd = (SerialBTDownloader)serialRead.get(6);
            Map<String, Serializable> properties = sbtd.getProperties();
            assertEquals("btName", properties.get("defaultFileName"));
            BTMetaInfo info = (BTMetaInfo)properties.get("metainfo");
            assertEquals("http://www.example.com/announce", info.getTrackers()[0].toString());
            assertEquals(UrnHelper.URNS[3].getBytes(), info.getInfoHash());
            assertEquals(123L, info.getFileSystem().getTotalSize());
            assertEquals(1235L, info.getPieceLength());
            assertEquals(true, info.isPrivate());
        }

    }

//    public void testCreateFiles() throws Exception {
//        Injector injector = LimeTestUtils.createInjector();
//        GnutellaDownloaderFactory gnutellaDownloadFactory = injector
//                .getInstance(GnutellaDownloaderFactory.class);
//        PurchasedStoreDownloaderFactory purchasedStoreDownloaderFactory = injector
//                .getInstance(PurchasedStoreDownloaderFactory.class);
//        BTDownloaderFactory btDownloaderFactory = injector.getInstance(BTDownloaderFactory.class);
//        SearchServices searchServices = injector.getInstance(SearchServices.class);
//        DownloadReferencesFactory downloadReferencesFactory = injector
//                .getInstance(DownloadReferencesFactory.class);
//        IncompleteFileManager incompleteFileManager = injector
//                .getInstance(IncompleteFileManager.class);
//        File saveDirectory = new File("saveDirectory");
//        saveDirectory.mkdirs();
//        saveDirectory.deleteOnExit();
//
//        ManagedDownloader managedDownloader;
//        StoreDownloader storeDownloader;
//        InNetworkDownloader inNetworkDownloader;
//        ResumeDownloader resumeDownloader;
//        MagnetDownloader magnetDownloader;
//        RequeryDownloader requeryDownloader;
//        BTDownloader btDownloader;
//
//        byte[] guid = new GUID().bytes();
//        GUID queryGuid = new GUID(searchServices.newQueryGUID());
//        RemoteFileDesc rfd = new RemoteFileDesc("127.0.0.1", 1, 1, "fileA.txt", 123, guid, 1, true,
//                1, false, null, UrnHelper.URN_SETS[0], false, true, "MNGD", null, -1, false);
//        managedDownloader = gnutellaDownloadFactory.createManagedDownloader(
//                new RemoteFileDesc[] { rfd }, incompleteFileManager, queryGuid);
//        managedDownloader.setAttribute("KEY", "VALUE");
//
//        rfd = new RemoteFileDesc("127.0.0.2", 2, 1, "fileB.txt", 123, guid, 1, true, 1, false,
//                null, UrnHelper.URN_SETS[0], false, true, "STOR", null, -1, false);
//        storeDownloader = purchasedStoreDownloaderFactory.createStoreDownloader(rfd,
//                incompleteFileManager, saveDirectory, "name", false);
//
//        DownloadInformation downloadInformation = new DownloadInformation() {
//            public long getSize() {
//                return 12356;
//            }
//
//            public String getTTRoot() {
//                return TestFile.tree().getRootHash();
//            }
//
//            public String getUpdateCommand() {
//                return "";
//            }
//
//            public String getUpdateFileName() {
//                return "updateName";
//            }
//
//            public URN getUpdateURN() {
//                return TestFile.hash();
//            }
//        };
//        inNetworkDownloader = gnutellaDownloadFactory.createInNetworkDownloader(
//                incompleteFileManager, downloadInformation, saveDirectory, 1235);
//
//        resumeDownloader = gnutellaDownloadFactory.createResumeDownloader(incompleteFileManager,
//                new File("T-123453-incompleteName"), "incompleteName", 123453);
//
//        MagnetOptions magnet = MagnetOptions.createMagnet(new FileDetails() {
//            public String getFileName() {
//                return "magnetName";
//            }
//
//            public long getFileSize() {
//                return 6531;
//            }
//
//            public InetSocketAddress getInetSocketAddress() {
//                return new InetSocketAddress("127.0.0.3", 3);
//            }
//
//            public URN getSHA1Urn() {
//                return UrnHelper.URNS[1];
//            }
//
//            public Set<URN> getUrns() {
//                return UrnHelper.URN_SETS[1];
//            }
//
//            public LimeXMLDocument getXMLDocument() {
//                return null;
//            }
//
//            public boolean isFirewalled() {
//                return false;
//            }
//        });
//        magnetDownloader = gnutellaDownloadFactory.createMagnetDownloader(incompleteFileManager,
//                magnet, false, saveDirectory, "magnetName");
//
//        requeryDownloader = null;
//      // Requires RequeryDownloader is changed to not be deprecated
//      //  requeryDownloader = new RequeryDownloader(new RemoteFileDesc[] { rfd },
//      //          incompleteFileManager, queryGuid, injector.getInstance(SaveLocationManager.class));
//
//        BTData btData = new BTData() {
//            public void clearPieces() {
//            }
//
//            public String getAnnounce() {
//                return "http://www.example.com/announce";
//            }
//
//            public List<BTFileData> getFiles() {
//                return null;
//            }
//
//            public Set<String> getFolders() {
//                return null;
//            }
//
//            public byte[] getInfoHash() {
//                return UrnHelper.URNS[3].getBytes();
//            }
//
//            public Long getLength() {
//                return 123L;
//            }
//
//            public String getName() {
//                return "btName";
//            }
//
//            public Long getPieceLength() {
//                return 1235L;
//            }
//
//            public byte[] getPieces() {
//                return new byte[0];
//            }
//
//            public boolean isPrivate() {
//                return true;
//            }
//        };
//        btDownloader = btDownloaderFactory.createBTDownloader(new BTMetaInfo(btData));
//
//        AbstractDownloader[] allDownloaders = { managedDownloader, storeDownloader,
//                inNetworkDownloader, resumeDownloader, magnetDownloader, requeryDownloader,
//                btDownloader };
//
//        for (AbstractDownloader downloader : allDownloaders)
//            downloader.initialize(downloadReferencesFactory.create(downloader));
//        List<AbstractDownloader> downloaders = new ArrayList<AbstractDownloader>(Arrays
//                .asList(allDownloaders));
//        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(
//                "allKindsOfDownloads.dat")));
//        out.writeObject(downloaders);
//        out.writeObject(incompleteFileManager);
//        out.flush();
//        out.close();
//
//        // Make sure we can read it!!!
//        DownloadManager downloadManager = injector.getInstance(DownloadManager.class);
//        List<AbstractDownloader> read = downloadManager.readSnapshot(new File(
//                "allKindsOfDownloads.dat"));
//        assertEquals(7, read.size());
//    }

}
