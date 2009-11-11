package org.limewire.bittorrent;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BTDataImplTest extends BaseTestCase {

    public void testParseTrackerUrisSingleAnnounce() throws Exception {
        Map<?,?> data = ImmutableMap.of("announce", StringUtils.toAsciiBytes("http://me.you/?t=5"));
        List<URI> uris = BTDataImpl.parseTrackerUris(data);
        assertEquals(ImmutableList.of(URIUtils.toURI("http://me.you/?t=5")), uris);
    }
    
    public void testParseTrackerUrisAnnounceList() throws Exception {
        Map<?,?> data = ImmutableMap.of("announce-list", ImmutableList.of(ImmutableList.of(StringUtils.toAsciiBytes("http://1/"), StringUtils.toAsciiBytes("http://2/"))));
        List<URI> uris = BTDataImpl.parseTrackerUris(data);
        assertEquals(ImmutableList.of(URIUtils.toURI("http://1/"), URIUtils.toURI("http://2/")), uris);
    }

    /**
     * Ensures that old-style announce is ignored if announce list is in the torrent
     */
    public void testParseTrackerUrisAnnounceListIgnoreAnnounce() throws Exception {
        Map<?,?> data = ImmutableMap.of("announce-list", ImmutableList.of(ImmutableList.of(StringUtils.toAsciiBytes("http://1/"), StringUtils.toAsciiBytes("http://2/"))),
                "announce", StringUtils.toAsciiBytes("http://ignored.url/"));
        List<URI> uris = BTDataImpl.parseTrackerUris(data);
        assertEquals(ImmutableList.of(URIUtils.toURI("http://1/"), URIUtils.toURI("http://2/")), uris);
    }
    
}
