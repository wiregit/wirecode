package org.limewire.core.impl;

import java.util.List;

import junit.framework.Test;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;


public class TorrentFactoryImplTest extends BaseTestCase {

    private TorrentFactory torrentFactory;
    private LimeXMLDocumentFactory xmlFactory;
    
    public TorrentFactoryImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TorrentFactoryImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjectorNonEagerly(new CoreGlueModule());
        torrentFactory = injector.getInstance(TorrentFactory.class);
        xmlFactory = injector.getInstance(LimeXMLDocumentFactory.class);
    }
    
    public void testWrongXML() throws Exception {
        LimeXMLDocument videoXML = xmlFactory.createLimeXMLDocument(
                FileManagerTestUtils.buildVideoXMLString("director=\"one interesting tree\" title=\"Eight Four David\""));
        Torrent torrent = torrentFactory.createTorrentFromXML(videoXML);
        
        assertNull(torrent);
    }
    
    public void testTorrentXMLNoFiles() throws Exception {
        LimeXMLDocument nine = xmlFactory.createLimeXMLDocument(
                FileManagerTestUtils.buildTorrentXMLString("infohash=\"1234GPLQSDW51234FDQQXXM5HYT51234\" " +
                                "trackers=\"http://tracker.legaltorrents.org/announce\" " +
                                "name=\"some_file_name\""));
        Torrent torrent = torrentFactory.createTorrentFromXML(nine);
        
        assertNull(torrent);
    }

    public void testTorrentXML() throws Exception {
        LimeXMLDocument nine = xmlFactory.createLimeXMLDocument(
                FileManagerTestUtils.buildTorrentXMLString("infohash=\"1234GPLQSDW51234FDQQXXM5HYT51234\" " +
                                "trackers=\"http://tracker.legaltorrents.org/announce\" " +
                                "name=\"some_file_name\" " + 
                                "filepaths=\"/somepath.txt///somefile.txt///someotherfile.txt\" " + 
                                "filesizes=\"123 234 456\""));
        Torrent torrent = torrentFactory.createTorrentFromXML(nine);
        
        assertNotNull(torrent);
        assertEquals("some_file_name",torrent.getName());
        assertEquals(getHash("1234GPLQSDW51234FDQQXXM5HYT51234"), torrent.getSha1());
        assertEquals("http://tracker.legaltorrents.org/announce", torrent.getTrackerURL());
        assertEquals(3, torrent.getTorrentFileEntries().size());
        List<TorrentFileEntry> entries = torrent.getTorrentFileEntries();
        assertEquals(123, entries.get(0).getSize());
        assertEquals("somepath.txt", entries.get(0).getPath());
        assertEquals(234, entries.get(1).getSize());
        assertEquals("somefile.txt", entries.get(1).getPath());
        assertEquals(456, entries.get(2).getSize());
        assertEquals("someotherfile.txt", entries.get(2).getPath());
    }
    
    
    private String getHash(String hash) {
        byte[] bytes = Base32.decode(hash);
        return StringUtils.toHexString(bytes);
    }
}
