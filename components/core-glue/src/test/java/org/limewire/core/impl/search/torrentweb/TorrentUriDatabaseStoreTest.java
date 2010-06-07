package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.Clock;
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
        torrentUriDatabaseStore.addCanonicalTorrentUri(host, uri);
        torrentUriDatabaseStore.addCanonicalTorrentUri(host, uri);
        
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
    
    public void testStoreRobotsTxt() {
        String robotsTxt = "User-agent: *\nAllow: /\n";
        torrentUriDatabaseStore.storeRobotsTxt("host.info", robotsTxt);
        assertEquals(robotsTxt, torrentUriDatabaseStore.getRobotsTxt("host.info"));
        assertNull(torrentUriDatabaseStore.getRobotsTxt("unknown.host.com"));
        torrentUriDatabaseStore.stop();
        
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(new ClockImpl());
        assertEquals(robotsTxt, torrentUriDatabaseStore.getRobotsTxt("host.info"));
    }
    
    public void testPurgesOldRobotsTxtEntriesAfterTwoWeeks() {
        String robotsTxt = "User-agent: *\nAllow: /\n";
        torrentUriDatabaseStore.storeRobotsTxt("host.info", robotsTxt);
        torrentUriDatabaseStore.stop();
        
        // still has entry
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(13));
        assertEquals(robotsTxt, torrentUriDatabaseStore.getRobotsTxt("host.info"));
        torrentUriDatabaseStore.stop();
        
        // entry purged
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(15));
        assertNull(torrentUriDatabaseStore.getRobotsTxt("host.info"));
    }
    
    public void testPurgesOldUriEntries() {
        URI uri = URI.create("http://torrent.uri/");
        torrentUriDatabaseStore.setIsTorrentUri(uri, true);
        torrentUriDatabaseStore.stop();
        
        // entry still there
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(89));
        assertTrue(torrentUriDatabaseStore.isTorrentUri(uri));
        torrentUriDatabaseStore.stop();
        
        // entry purged
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(91));
        assertFalse(torrentUriDatabaseStore.isTorrentUri(uri));
    }
    
    public void testChangingTorrentUriUpdatesTimestamp() {
        URI uri = URI.create("http://torrent.uri/");
        torrentUriDatabaseStore.setIsTorrentUri(uri, true);
        torrentUriDatabaseStore.stop();
        
        // entry still there
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(89));
        assertTrue(torrentUriDatabaseStore.isTorrentUri(uri));
        // change entry
        torrentUriDatabaseStore.setIsTorrentUri(uri, false);
        assertTrue(torrentUriDatabaseStore.isNotTorrentUri(uri));
        torrentUriDatabaseStore.stop();
        
        // entry purged
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(91));
        assertTrue(torrentUriDatabaseStore.isNotTorrentUri(uri));
    }
    
    public void testPurgesOldUriByHostEntries() {
        URI uri = URI.create("http://torrent.uri/");
        torrentUriDatabaseStore.addCanonicalTorrentUri("torrent.uri", uri);
        torrentUriDatabaseStore.stop();
        
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(89));
        assertContains(torrentUriDatabaseStore.getTorrentUrisForHost("torrent.uri"), uri);
        torrentUriDatabaseStore.stop();
        
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(91));
        assertEmpty(torrentUriDatabaseStore.getTorrentUrisForHost("torrent.uri"));
    }
    
    public void testReaddingUriByHostUpdatesTimestamp() {
        URI uri = URI.create("http://torrent.uri/");
        torrentUriDatabaseStore.addCanonicalTorrentUri("torrent.uri", uri);
        torrentUriDatabaseStore.stop();
        
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(89));
        assertContains(torrentUriDatabaseStore.getTorrentUrisForHost("torrent.uri"), uri);
        // refresh entry        
        torrentUriDatabaseStore.addCanonicalTorrentUri("torrent.uri", uri);
        torrentUriDatabaseStore.stop();
        
        torrentUriDatabaseStore = new TorrentUriDatabaseStore(createFutureClock(91));
        assertContains(torrentUriDatabaseStore.getTorrentUrisForHost("torrent.uri"), uri);
    }
    
    private Clock createFutureClock(final long  days) {
        Mockery context = new Mockery();
        final Clock clock = context.mock(Clock.class);
        context.checking(new Expectations() {{
            allowing(clock).now();
            will(returnValue(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)));
        }});
        return clock;
    }
}
