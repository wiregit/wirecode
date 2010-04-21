package org.limewire.core.impl;

import java.util.List;

import junit.framework.Test;

import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.InvalidDataException;

import com.google.inject.Injector;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public class XMLTorrentTest extends LimeTestCase {
    
    private LimeXMLDocumentFactory xmlFactory;

    public static Test suite() {
        return buildTestSuite(XMLTorrentTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjectorNonEagerly(new CoreGlueModule());
        xmlFactory = injector.getInstance(LimeXMLDocumentFactory.class);
    }
    
    
    public void testTorrentXmlWithoutNameFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
    
    public void testTorrentXmlWithoutLengthFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
    
    public void testTorrentXmlWithSingleFile() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" length=\"2545\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        XMLTorrent torrent = new XMLTorrent(xmlDocument);
        assertEquals("name", torrent.getName());
        List<TorrentFileEntry> entries = torrent.getTorrentFileEntries();
        assertEquals(1, entries.size());
        assertEquals("name", entries.get(0).getPath());
        assertEquals(2545, entries.get(0).getSize());
        assertEquals(2545, torrent.getTotalSize());
    }
    
    public void testTorrentXmlWithLackingFileSizesFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"/file1///file2\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
        xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"/file1///file2\" filesizes=\"1234\"/></torrents>";
        xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
    
    public void testTorrentXmlWithCorrectFileSizes() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"/file1///file2\" filesizes=\"1000 2000\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        XMLTorrent torrent = new XMLTorrent(xmlDocument);
        List<TorrentFileEntry> entries = torrent.getTorrentFileEntries();
        assertEquals(2, entries.size());
        assertEquals("file1", entries.get(0).getPath());
        assertEquals(1000, entries.get(0).getSize());
        assertEquals("file2", entries.get(1).getPath());
        assertEquals(2000, entries.get(1).getSize());
        assertEquals(3000, torrent.getTotalSize());
    }
    
    public void testTorrentXmlWithInvalidTrackersFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"/file1///file2\" filesizes=\"1000 2000\" trackers=\"udp:// ,!lkdfll\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
    
    public void testTorrentXmlWithoutInfoHashFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent binfohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"/file1///file2\" filesizes=\"1000 2000\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
    
    public void testTorrentXmlWithEmptyPathsFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"///file2\" filesizes=\"1000 2000\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
    
    public void testTorrentXmlWithInvalidInfoHashFails() throws Exception {
        String xml = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"OWVPM7JN7ZDRX6HFMZAPJPHI3JXIUUZ\" name=\"name\" filepaths=\"/file1///file2\" filesizes=\"1000 2000\"/></torrents>";
        LimeXMLDocument xmlDocument = xmlFactory.createLimeXMLDocument(xml);
        try {
            new XMLTorrent(xmlDocument);
            fail("exception expected");
        } catch (InvalidDataException e) {
        }
    }
}
