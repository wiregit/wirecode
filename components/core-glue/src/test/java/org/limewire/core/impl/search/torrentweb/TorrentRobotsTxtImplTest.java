package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;

public class TorrentRobotsTxtImplTest extends LimeTestCase {
    
    @Inject
    private TorrentRobotsTxtImpl torrentRobotsTxtImpl;
    
    private final MockTorrentRobotsTxtStore store = new MockTorrentRobotsTxtStore();
    
    public static Test suite() { 
        return buildTestSuite(TorrentRobotsTxtImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        LimeTestUtils.createInjector(LimeTestUtils.createModule(this),
                TestUtils.bind(TorrentRobotsTxtStore.class).toInstances(store));
    }
    
    public void testLimeWireCom() {
        assertTrue(torrentRobotsTxtImpl.isAllowed(URI.create("http://limewire.com/download/")));
    }
    
    public void testGoogleCom() {
        assertFalse(torrentRobotsTxtImpl.isAllowed(URI.create("http://google.com/search")));
        assertTrue(torrentRobotsTxtImpl.isAllowed(URI.create("http://google.com/")));
        assertTrue(torrentRobotsTxtImpl.isAllowed(URI.create("http://google.com/news/directory")));
    }
    
    public void testUsesLocalStore() {
        TorrentRobotsTxtImpl torrentRobotsTxtImpl = new TorrentRobotsTxtImpl(null, store);
        store.storeRobotsTxt("limewire.com", "User-agent: *\nDisallow: /");
        assertFalse(torrentRobotsTxtImpl.isAllowed(URI.create("http://LIMEWIRE.com/")));
        assertFalse(torrentRobotsTxtImpl.isAllowed(URI.create("http://LIMEWIRE.com/download/")));
    }
        
    private static class MockTorrentRobotsTxtStore implements TorrentRobotsTxtStore {
        
        Map<String, String> entries = new HashMap<String, String>();

        @Override
        public String getRobotsTxt(String host) {
            return entries.get(host);
        }

        @Override
        public void storeRobotsTxt(String host, String robotsTxt) {
            entries.put(host, robotsTxt);
        }
        
    }
}
