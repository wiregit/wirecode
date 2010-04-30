package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.Set;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.ClockImpl;

public class TorrentUriDatabaseStoreTest extends LimeTestCase {

    private TorrentUriDatabaseStore torrentUriDatabaseStore;

    public static Test suite() { 
        return buildTestSuite(TorrentUriDatabaseStoreTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(new ClockImpl());
    }
    
    @Override
    protected void tearDown() throws Exception {
        torrentUriDatabaseStore.stop();
    }
    
    public void testCanonicalTorrentUrisAreUnique() {
        URI uri = URI.create("http://torrent/url/canonical");
        String host = "torrent";
        torrentUriDatabaseStore.addCanonicalTorrentUris(host, uri);
        torrentUriDatabaseStore.addCanonicalTorrentUris(host, uri);
        
        Set<URI> uris = torrentUriDatabaseStore.getTorrentUrisForHost(host);
        assertEquals(1, uris.size());
        assertContains(uris, uri);
    }
    
    public void testStoresDataAcrossSessions() {
        URI uri = URI.create("http://torrent/url/download");
        torrentUriDatabaseStore.setIsTorrentUri(uri, true);
        assertTrue(torrentUriDatabaseStore.isTorrentUri(uri));
        torrentUriDatabaseStore.stop();
        
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(new ClockImpl());
        assertTrue(torrentUriDatabaseStore.isTorrentUri(uri));
    }
    
    public void testIsTorrentNotTorrentUri() {
        URI uri = URI.create("http://torrent/login");
        torrentUriDatabaseStore.setIsTorrentUri(uri, false);
        assertTrue(torrentUriDatabaseStore.isNotTorrentUri(uri));
        assertFalse(torrentUriDatabaseStore.isTorrentUri(uri));
        assertFalse(torrentUriDatabaseStore.isNotTorrentUri(URI.create("http://unknown/")));
    }
    
    public void testIsTorrentTorrentUri() {
        URI uri = URI.create("http://torrent/login");
        torrentUriDatabaseStore.setIsTorrentUri(uri, true);
        assertTrue(torrentUriDatabaseStore.isTorrentUri(uri));
        assertFalse(torrentUriDatabaseStore.isNotTorrentUri(uri));
        assertFalse(torrentUriDatabaseStore.isTorrentUri(URI.create("http://unknown/")));
    }
    
    public void testSetTorrentUriOverridesOlderEntry() {
        URI uri = URI.create("http://torrent/login");
        torrentUriDatabaseStore.setIsTorrentUri(uri, false);
        torrentUriDatabaseStore.setIsTorrentUri(uri, true);
        assertTrue(torrentUriDatabaseStore.isTorrentUri(uri));

        torrentUriDatabaseStore.setIsTorrentUri(uri, false);
        assertFalse(torrentUriDatabaseStore.isTorrentUri(uri));
        assertTrue(torrentUriDatabaseStore.isNotTorrentUri(uri));
    }
}
