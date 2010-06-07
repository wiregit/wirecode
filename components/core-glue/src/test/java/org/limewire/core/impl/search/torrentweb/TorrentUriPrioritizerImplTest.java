package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

public class TorrentUriPrioritizerImplTest extends BaseTestCase {
    
    public static Test suite() { 
        return buildTestSuite(TorrentUriPrioritizerImplTest.class);
    }

    private Mockery context;
    private TorrentUriStore torrentUriStore;
    private TorrentUriPrioritizerImpl torrentUriPrioritizerImpl;
    
    private Expectations emptyUriStoreExpectations;
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        torrentUriStore = context.mock(TorrentUriStore.class);
        torrentUriPrioritizerImpl = new TorrentUriPrioritizerImpl(URI.create("http://referrer.com/referrer"), "multi word query", torrentUriStore);
        
        emptyUriStoreExpectations = new Expectations(){{
            allowing(torrentUriStore).isNotTorrentUri(with(any(URI.class)));
            will(returnValue(false));
            allowing(torrentUriStore).isTorrentUri(with(any(URI.class)));
            will(returnValue(false));
            allowing(torrentUriStore).getTorrentUrisForHost(with(any(String.class)));
            will(returnValue(Collections.emptySet()));
        }};
    }
    
    public void testRemovesAllNonTorrentUris() {
        context.checking(new Expectations(){{
            allowing(torrentUriStore).isNotTorrentUri(with(any(URI.class)));
            will(returnValue(true));
        }});
        assertEmpty(torrentUriPrioritizerImpl.prioritize(Arrays.asList(URI.create("http://test/"), URI.create("http://again.com/no"))));
    }
    
    public void testTorrentUrisAreFirst() {
        final URI torrentUri = URI.create("http://torrent/torrenturi");
        context.checking(new Expectations(){{
            allowing(torrentUriStore).isNotTorrentUri(with(any(URI.class)));
            will(returnValue(false));
            one(torrentUriStore).isTorrentUri(torrentUri);
            will(returnValue(true));
            allowing(torrentUriStore).isTorrentUri(with(any(URI.class)));
            will(returnValue(false));
            allowing(torrentUriStore).getTorrentUrisForHost(with(any(String.class)));
            will(returnValue(Collections.emptySet()));
        }});
        
        List<URI> prioritized = torrentUriPrioritizerImpl.prioritize(Arrays.asList(URI.create("http://unknown/uri"), torrentUri, URI.create("http://another/unknown/uri")));
        assertEquals(torrentUri, prioritized.get(0));
    }
    
    public void testUrisSimilarToTorrentUrisAreFirst() {
        context.checking(new Expectations(){{
            allowing(torrentUriStore).isNotTorrentUri(with(any(URI.class)));
            will(returnValue(false));
            allowing(torrentUriStore).isTorrentUri(with(any(URI.class)));
            will(returnValue(false));
            allowing(torrentUriStore).getTorrentUrisForHost("torrent");
            will(returnValue(Collections.singleton(URI.create("http://torrent/*query*/*numbers*/"))));
        }});
        URI uriWithQuery = URI.create("http://torrent/multi%20query%20word/1234/");
        List<URI> prioritized = torrentUriPrioritizerImpl.prioritize(Arrays.asList(URI.create("http://torrent/unknown/torrent"), URI.create("http://torrent/andanotherone/test"), uriWithQuery));
        assertEquals(uriWithQuery, prioritized.get(0));
    }
    
    public void testUrisEndingWithTorrentAreFirst() {
        context.checking(emptyUriStoreExpectations);
        URI uriEndingWithTorrent = URI.create("http://uri/ending/with.torrent");
        List<URI> prioritized = torrentUriPrioritizerImpl.prioritize(Arrays.asList(URI.create("http://torrent/unknown/torrent"), URI.create("http://torrent/andanotherone/test"), uriEndingWithTorrent));
        assertEquals(uriEndingWithTorrent, prioritized.get(0));
    }
    
    public void testUrisWithQueryAreFirst() {
        context.checking(emptyUriStoreExpectations);
        URI uriContainingQuery = URI.create("http://multi/word%20query");
        List<URI> prioritized = torrentUriPrioritizerImpl.prioritize(Arrays.asList(URI.create("http://torrent/unknown/torrent"), URI.create("http://torrent/andanotherone/test"), uriContainingQuery));
        assertEquals(uriContainingQuery, prioritized.get(0));
    }
    
    public void testCapsTop20Uris() {
        context.checking(emptyUriStoreExpectations);
        List<URI> uris = new ArrayList<URI>();
        for (int i = 0; i < 50; i++) {
            uris.add(URI.create("http://numbered/uri/" + i));
        }
        assertEquals(50, uris.size());
        List<URI> prioritized = torrentUriPrioritizerImpl.prioritize(uris);
        assertEquals(20, prioritized.size());
    }
}
