package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMemento;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.helpers.UrnHelper;

public class OldDownloadConverterImplTest extends BaseTestCase {

    public OldDownloadConverterImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OldDownloadConverterImplTest.class);
    }

    public void testConversionForTypes() throws Exception {
        File file = LimeTestUtils.getResourceInPackage("allKindsofDownloads.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl();
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(5, mementos.size());
        
        DownloadMemento read1 = mementos.get(0);
        DownloadMemento read2 = mementos.get(1);
        DownloadMemento read3 = mementos.get(2);
        DownloadMemento read4 = mementos.get(3);
        DownloadMemento read5 = mementos.get(4);
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read1;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\Sam\\Incomplete\\T-123-fileA.txt"), mem.getIncompleteFile());
            assertEquals(0, mem.getRanges().size());
            assertEquals(1, mem.getRemoteHosts().size());
            RemoteHostMemento rmem = mem.getRemoteHosts().iterator().next();
            assertEquals("127.0.0.1", rmem.getHost());
            assertEquals(1, rmem.getPort());
            assertEquals(1, rmem.getIndex());
            assertEquals("fileA.txt", rmem.getFileName());
            assertEquals(123, rmem.getSize());
            // CHECK GUID!!!!
            assertEquals(1, rmem.getSpeed());
            assertEquals(true, rmem.isChat());
            assertEquals(1, rmem.getQuality());
            assertEquals(false, rmem.isBrowseHost());
            assertEquals(null, rmem.getXml());
            assertEquals(UrnHelper.URN_SETS[0], rmem.getUrns());
            assertEquals(false, rmem.isReplyToMulticast());
            assertEquals(true, rmem.isFirewalled());
            assertEquals("MNGD", rmem.getVendor());
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            assertEquals("VALUE", attributes.get("KEY"));
            assertEquals(123L, properties.get("fileSize"));
            assertEquals("fileA.txt", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[0], properties.get("sha1Urn"));
        }
            
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read2;
            assertEquals(DownloaderType.STORE, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\Sam\\Incomplete\\T-123-fileA.txt"), mem.getIncompleteFile());
            assertEquals(0, mem.getRanges().size());
            assertEquals(1, mem.getRemoteHosts().size());
            RemoteHostMemento rmem = mem.getRemoteHosts().iterator().next();
            assertEquals("127.0.0.2", rmem.getHost());
            assertEquals(2, rmem.getPort());
            assertEquals(1, rmem.getIndex());
            assertEquals("fileB.txt", rmem.getFileName());
            assertEquals(123, rmem.getSize());
            // CHECK GUID!!!!
            assertEquals(1, rmem.getSpeed());
            assertEquals(true, rmem.isChat());
            assertEquals(1, rmem.getQuality());
            assertEquals(false, rmem.isBrowseHost());
            assertEquals(null, rmem.getXml());
            assertEquals(UrnHelper.URN_SETS[0], rmem.getUrns());
            assertEquals(false, rmem.isReplyToMulticast());
            assertEquals(true, rmem.isFirewalled());
            assertEquals("STOR", rmem.getVendor());
            Map<String, Serializable> properties = mem.getPropertiesMap();
            assertEquals(new HashMap(), properties.get("attributes"));
            assertEquals(123L, properties.get("fileSize"));
            assertEquals("fileB.txt", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[0], properties.get("sha1Urn"));
        }
        
          
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read3;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            // Normally the incompleteFile would be canonical -- we just didn't create it
            // that way in the test file.
            assertEquals(new File("T-123453-incompleteName"), mem.getIncompleteFile());
            assertEquals(0, mem.getRanges().size());
            assertEquals(0, mem.getRemoteHosts().size());            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            assertEquals(new HashMap(), properties.get("attributes"));
            assertEquals(123453L, properties.get("fileSize"));
            assertEquals("incompleteName", properties.get("defaultFileName"));
            assertEquals(null, properties.get("sha1Urn"));
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read4;
            assertEquals(DownloaderType.MAGNET, mem.getDownloadType());
            // Null because the download never started.
            assertEquals(null, mem.getIncompleteFile());
            assertEquals(0, mem.getRanges().size());
            assertEquals(0, mem.getRemoteHosts().size());            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            assertEquals(null, properties.get("fileSize"));
            assertEquals("magnetName", properties.get("defaultFileName"));
            assertEquals(UrnHelper.URNS[1], properties.get("sha1Urn"));
            assertEquals(new HashMap(), properties.get("attributes"));
            
            MagnetOptions mo = (MagnetOptions)properties.get("MAGNET");
            assertEquals("magnetName", mo.getDisplayName());
            assertEquals(UrnHelper.URNS[1], mo.getSHA1Urn());
            assertEquals(Collections.singletonList("http://127.0.0.3:3/uri-res/N2R?" + UrnHelper.URNS[1]), mo.getXS());
        }
        
        {
            BTDownloadMemento mem = (BTDownloadMemento)read5;
            assertEquals(DownloaderType.BTDOWNLOADER, mem.getDownloadType());
            Map<String, Serializable> properties = mem.getPropertiesMap();
            assertEquals("btName", properties.get("defaultFileName"));
            BTMetaInfo info = (BTMetaInfo)properties.get("metainfo");
            assertEquals("http://www.example.com/announce", info.getTrackers()[0].toString());
            assertEquals(UrnHelper.URNS[3].getBytes(), info.getInfoHash());
            assertEquals(123L, info.getFileSystem().getTotalSize());
            assertEquals(1235L, info.getPieceLength());
            assertEquals(true, info.isPrivate());
        }        
    }
    
    public void testConversionForRanges() throws Exception {
        File file = LimeTestUtils.getResourceInPackage("allKindsofRanges.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl();
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(6, mementos.size());
        
        DownloadMemento read1 = mementos.get(0);
        DownloadMemento read2 = mementos.get(1);
        DownloadMemento read3 = mementos.get(2);
        DownloadMemento read4 = mementos.get(3);
        DownloadMemento read5 = mementos.get(4);
        DownloadMemento read6 = mementos.get(5);
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read1;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-4495072-LimeWireWin4.16.0.exe"), mem.getIncompleteFile());
            assertEquals(1, mem.getRanges().size());
            assertEquals(Range.createRange(3276800, 3316399), mem.getRanges().get(0));
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
            assertEquals("*", so.getMediaType().getMimeType());
            assertEquals("limewire", so.getQuery());
            assertEquals(null, so.getXML());
            assertTrue(so.isKeywordSearch());
            assertEquals("limewire", so.getTitle());
            assertEquals(4495072L, properties.get("fileSize"));
            assertEquals("LimeWireWin4.16.0.exe", properties.get("defaultFileName"));
            assertEquals(URN.createSHA1Urn("urn:sha1:A6DGMXEOJDBQOIJUQTAQWSWC2IQKFD5J"), properties.get("sha1Urn"));
            
            assertEquals(2, mem.getRemoteHosts().size());
            Iterator<RemoteHostMemento> mementoIterator = mem.getRemoteHosts().iterator();            
            RemoteHostMemento rmem = mementoIterator.next();
            RemoteHostMemento rmem2 = mementoIterator.next();
            
            // Since remoteHosts is a HashSet, it can be out-of-order..
            if(!rmem.getHost().equals("92.1.246.69")) {
                RemoteHostMemento tmp = rmem;
                rmem = rmem2;
                rmem2 = tmp;
            }   
            
            assertEquals("92.1.246.69", rmem.getHost());
            assertEquals(6346, rmem.getPort());
            assertEquals(4, rmem.getIndex());
            assertEquals("LimeWireWin4.16.0.exe", rmem.getFileName());
            assertEquals(4495072L, rmem.getSize());
            // CHECK GUID!!!!
            assertEquals(960, rmem.getSpeed());
            assertEquals(false, rmem.isChat());
            assertEquals(3, rmem.getQuality());
            assertEquals(true, rmem.isBrowseHost());
            assertEquals(null, rmem.getXml());
            assertEquals(new UrnSet(URN.createSHA1Urn("urn:sha1:A6DGMXEOJDBQOIJUQTAQWSWC2IQKFD5J")), rmem.getUrns());
            assertEquals(false, rmem.isReplyToMulticast());
            assertEquals(false, rmem.isFirewalled());
            assertEquals("LIME", rmem.getVendor());
            
            assertEquals("10.254.0.101", rmem2.getHost());
            assertEquals(33053, rmem2.getPort());
            assertEquals(0, rmem2.getIndex());
            assertEquals("LimeWireWin4.16.0.exe", rmem2.getFileName());
            assertEquals(4495072L, rmem2.getSize());
            // CHECK GUID!!!!
            assertEquals(2147483647, rmem2.getSpeed());
            assertEquals(false, rmem2.isChat());
            assertEquals(4, rmem2.getQuality());
            assertEquals(true, rmem2.isBrowseHost());
            assertEquals(null, rmem2.getXml());
            assertEquals(new UrnSet(URN.createSHA1Urn("urn:sha1:A6DGMXEOJDBQOIJUQTAQWSWC2IQKFD5J")), rmem2.getUrns());
            assertEquals(true, rmem2.isReplyToMulticast());
            assertEquals(false, rmem2.isFirewalled());
            assertEquals("LIME", rmem2.getVendor());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read2;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-4400168-LimeWireWin4.15.5.exe"), mem.getIncompleteFile());
            assertEquals(3, mem.getRanges().size());
            assertEquals(Range.createRange(0, 16895), mem.getRanges().get(0));
            assertEquals(Range.createRange(786432, 823029), mem.getRanges().get(1));
            assertEquals(Range.createRange(3801088, 3932159), mem.getRanges().get(2));
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
            assertEquals("*", so.getMediaType().getMimeType());
            assertEquals("limewire", so.getQuery());
            assertEquals(null, so.getXML());
            assertTrue(so.isKeywordSearch());
            assertEquals("limewire", so.getTitle());
            assertEquals(4400168L, properties.get("fileSize"));
            assertEquals("LimeWireWin4.15.5.exe", properties.get("defaultFileName"));
            assertEquals(URN.createSHA1Urn("urn:sha1:ZKPIRLABHCFSNTMOFO7AK7FFVVIHBRQO"), properties.get("sha1Urn"));            

            assertEquals(2, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read3;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-3381280-LimeWireWin4.14.12.exe"), mem.getIncompleteFile());
            assertEquals(6, mem.getRanges().size());
            assertEquals(Range.createRange(0, 23551), mem.getRanges().get(0));
            assertEquals(Range.createRange(524288, 634879), mem.getRanges().get(1));
            assertEquals(Range.createRange(1048576, 1050623), mem.getRanges().get(2));
            assertEquals(Range.createRange(1572864, 1667071), mem.getRanges().get(3));
            assertEquals(Range.createRange(2621440, 2661103), mem.getRanges().get(4));
            assertEquals(Range.createRange(3145728, 3381279), mem.getRanges().get(5));
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
            assertEquals("*", so.getMediaType().getMimeType());
            assertEquals("limewire", so.getQuery());
            assertEquals(null, so.getXML());
            assertTrue(so.isKeywordSearch());
            assertEquals("limewire", so.getTitle());
            assertEquals(3381280L, properties.get("fileSize"));
            assertEquals("LimeWireWin4.14.12.exe", properties.get("defaultFileName"));
            assertEquals(URN.createSHA1Urn("urn:sha1:SROVXQRNE6ZA6N26OKL6BMERSAIO4HVE"), properties.get("sha1Urn"));            

            assertEquals(9, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read4;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-2305127-LimeWirePackedJars4.12.6.7z"), mem.getIncompleteFile());
            assertEquals(4, mem.getRanges().size());
            assertEquals(Range.createRange(262144, 263543), mem.getRanges().get(0));
            assertEquals(Range.createRange(524288, 1310719), mem.getRanges().get(1));
            assertEquals(Range.createRange(1572864, 1654783), mem.getRanges().get(2));
            assertEquals(Range.createRange(1835008, 2097151), mem.getRanges().get(3));
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
            assertEquals("*", so.getMediaType().getMimeType());
            assertEquals("limewire", so.getQuery());
            assertEquals(null, so.getXML());
            assertTrue(so.isKeywordSearch());
            assertEquals("limewire", so.getTitle());
            assertEquals(2305127L, properties.get("fileSize"));
            assertEquals("LimeWirePackedJars4.12.6.7z", properties.get("defaultFileName"));
            assertEquals(URN.createSHA1Urn("urn:sha1:XOOJZHTKRTKTIFHOHXYOEXVAJPYVAGDE"), properties.get("sha1Urn"));            

            assertEquals(3, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read5;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-3380048-LimeWireWin4.14.10.exe"), mem.getIncompleteFile());
            assertEquals(5, mem.getRanges().size());
            assertEquals(Range.createRange(0, 99703), mem.getRanges().get(0));
            assertEquals(Range.createRange(262144, 292328), mem.getRanges().get(1));
            assertEquals(Range.createRange(1048576, 1049087), mem.getRanges().get(2));
            assertEquals(Range.createRange(1310720, 2621439), mem.getRanges().get(3));
            assertEquals(Range.createRange(3145728, 3238639), mem.getRanges().get(4));
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
            assertEquals("*", so.getMediaType().getMimeType());
            assertEquals("limewire", so.getQuery());
            assertEquals(null, so.getXML());
            assertTrue(so.isKeywordSearch());
            assertEquals("limewire", so.getTitle());
            assertEquals(3380048L, properties.get("fileSize"));
            assertEquals("LimeWireWin4.14.10.exe", properties.get("defaultFileName"));
            assertEquals(URN.createSHA1Urn("urn:sha1:DSGYQ4XCX6VIIAHACM3JNY2UXREK7OGK"), properties.get("sha1Urn"));            

            assertEquals(11, mem.getRemoteHosts().size());
        }
        
        {
            GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read6;
            assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
            assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-3064200-LimeWireWin4.12.6.exe"), mem.getIncompleteFile());
            assertEquals(3, mem.getRanges().size());
            assertEquals(Range.createRange(262144, 490223), mem.getRanges().get(0));
            assertEquals(Range.createRange(1572864, 1886959), mem.getRanges().get(1));
            assertEquals(Range.createRange(2097152, 2191925), mem.getRanges().get(2));
            
            Map<String, Serializable> properties = mem.getPropertiesMap();
            Map<?, ?> attributes = (Map)properties.get("attributes");
            SearchInformation so = SearchInformation.createFromMap((Map)attributes.get("searchInformationMap"));
            assertEquals("*", so.getMediaType().getMimeType());
            assertEquals("limewire", so.getQuery());
            assertEquals(null, so.getXML());
            assertTrue(so.isKeywordSearch());
            assertEquals("limewire", so.getTitle());
            assertEquals(3064200L, properties.get("fileSize"));
            assertEquals("LimeWireWin4.12.6.exe", properties.get("defaultFileName"));
            assertEquals(URN.createSHA1Urn("urn:sha1:B3KUDG6BOAMIXEIFL6YCW27LH3A4ODL6"), properties.get("sha1Urn"));            

            assertEquals(3, mem.getRemoteHosts().size());
        }        
    }
    
    public void testMagnet() throws Exception {
        File file = LimeTestUtils.getResourceInPackage("magnet.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl();
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(1, mementos.size());
        
        DownloadMemento read = mementos.get(0);
        GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read;
        assertEquals(DownloaderType.MAGNET, mem.getDownloadType());
        assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-12229522-01-steve_winwood-dear_mr_fantasy-jun.mp3"), mem.getIncompleteFile());
        assertEquals(2, mem.getRanges().size());
        assertEquals(Range.createRange(0, 1310719), mem.getRanges().get(0));
        assertEquals(Range.createRange(4718592, 4890161), mem.getRanges().get(1));
        assertEquals(1, mem.getRemoteHosts().size());            
        Map<String, Serializable> properties = mem.getPropertiesMap();
        assertEquals(12229522L, properties.get("fileSize"));
        assertEquals("01-steve_winwood-dear_mr_fantasy-jun.mp3", properties.get("defaultFileName"));
        assertEquals(URN.createSHA1Urn("urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), properties.get("sha1Urn"));
        assertEquals(new HashMap(), properties.get("attributes"));        
        MagnetOptions mo = (MagnetOptions)properties.get("MAGNET");
        assertEquals("01-steve_winwood-dear_mr_fantasy-jun.mp3", mo.getDisplayName());
        assertEquals(URN.createSHA1Urn("urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), mo.getSHA1Urn());
        assertEquals(Collections.singletonList("http://64.61.25.138:6346/uri-res/N2R?urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), mo.getXS());
    }
    
    public void testXml() throws Exception {
        File file = LimeTestUtils.getResourceInPackage("xml.dat", DownloadUpgradeTask.class);
        
        OldDownloadConverterImpl oldDownloadConverter = new OldDownloadConverterImpl();
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(file);
        assertEquals(1, mementos.size());
        
        DownloadMemento read = mementos.get(0);
        GnutellaDownloadMemento mem = (GnutellaDownloadMemento)read;
        assertEquals(DownloaderType.MANAGED, mem.getDownloadType());
        assertEquals(new File("C:\\Documents and Settings\\sberlin\\My Documents\\LimeWire\\Incomplete\\T-12229522-01-steve_winwood-dear_mr_fantasy-jun.mp3"), mem.getIncompleteFile());
        assertEquals(1, mem.getRanges().size());
        assertEquals(Range.createRange(0, 131071), mem.getRanges().get(0));
        assertEquals(1, mem.getRemoteHosts().size());            
        Map<String, Serializable> properties = mem.getPropertiesMap();
        assertEquals(12229522L, properties.get("fileSize"));
        assertEquals("01-steve_winwood-dear_mr_fantasy-jun.mp3", properties.get("defaultFileName"));
        assertEquals(URN.createSHA1Urn("urn:sha1:VSHERRKVKU4FZVUDOB6UVERRC2BOEBG4"), properties.get("sha1Urn"));
        assertEquals("<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio year=\"2004\" title=\"Dear Mr. Fantasy\" artist=\"Steve Winwood\" album=\"Dear Mr. Fantasy Album Single\" genre=\"Classic Rock\" license=\"Access Hollywood\" seconds=\"509\" bitrate=\"192\" comments=\"Visit www.AccessHollywood.com/Abouttime for more free media content from Steve Winwood.\" track=\"1\"/></audios>",
                mem.getRemoteHosts().iterator().next().getXml());
    }
}
