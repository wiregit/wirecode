package com.limegroup.gnutella.metadata;

import java.io.File;
import java.util.List;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.NameValue;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.metadata.bittorrent.TorrentMetaData;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class MetaDataFactoryImplTest extends LimeTestCase {

    private MetaDataFactoryImpl metaDataFactory;

    public MetaDataFactoryImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MetaDataFactoryImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        metaDataFactory = new MetaDataFactoryImpl();
    }
    
    /**
     * Integration test to ensure {@link TorrentMetaReaderFactory} is registered
     * with {@link MetaDataFactoryImpl} and multi file torrents are parsed correctly.
     */
    @SuppressWarnings("null")
    public void testReadsMultiFileTorrentMetaData() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", getClass());
        assertTrue(torrentFile.exists());
        
        MetaData metaData = metaDataFactory.parse(torrentFile);
        assertTrue(metaData instanceof TorrentMetaData);
        List<NameValue<String>> values = metaData.toNameValueList();
        String filepaths = null;
        for (NameValue<String> value : values) {
            if (value.getName().equals(LimeXMLNames.TORRENT_FILE_PATHS)) {
                filepaths = value.getValue();
                break;
            }
        }
        assertNotNull(filepaths);
        String[] fileUris = filepaths.split("//");
        assertEquals(9, fileUris.length);
    }
    
    /**
     * Integration test to ensure {@link TorrentMetaReaderFactory} is registered
     * with {@link MetaDataFactoryImpl} and single file torrents are parsed correctly.
     */
    public void testReadsSingleFileTorrentMetaData() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("bthavetest.torrent", getClass());
        assertTrue(torrentFile.exists());
        
        MetaData metaData = metaDataFactory.parse(torrentFile);
        assertTrue(metaData instanceof TorrentMetaData);
        List<NameValue<String>> values = metaData.toNameValueList();
        String name = null;
        for (NameValue<String> value : values) {
            if (value.getName().equals(LimeXMLNames.TORRENT_NAME)) {
                name = value.getValue();
                break;
            }
        }
        assertEquals("BTHaveTest.class", name);
    }
}
