package com.limegroup.gnutella.metadata.bittorrent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTData.BTFileData;
import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;
import org.limewire.util.NameValue;
import org.limewire.util.URIUtils;

import com.google.common.collect.ImmutableList;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class TorrentMetaDataTest extends BaseTestCase {

    private Mockery context;

    public TorrentMetaDataTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TorrentMetaDataTest.class);
        
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    public void testToNameValueList() throws Exception {
        final BTData data = context.mock(BTData.class);
        final byte[] infoHash = new byte[20];
        new Random().nextBytes(infoHash);
        context.checking(new Expectations() {{ 
           allowing(data).getInfoHash();
           will(returnValue(infoHash));
           allowing(data).getTrackerUris();
           will(returnValue(ImmutableList.of(URIUtils.toURI("http://localhost/announce"), URIUtils.toURI("http://test/comma,path"))));
           allowing(data).getLength();
           will(returnValue(300L));
           allowing(data).getName();
           will(returnValue("torrent file"));
           allowing(data).getPieceLength();
           will(returnValue(2L));
           allowing(data).isPrivate();
           will(returnValue(true));
           allowing(data).getWebSeeds();
           will(returnValue(getUris()));
           allowing(data).getFiles();
           will(returnValue(Arrays.asList(new BTFileData(200L, "/home/test me \"hello world\""), new BTFileData(100L, "/home/me/\u30d5\u30a1"))));
        }});
        
        List<NameValue<String>> values = new TorrentMetaData(data).toNameValueList();
        assertContainsNameValue(values, LimeXMLNames.TORRENT_INFO_HASH, Base32.encode(infoHash));
        assertContainsNameValue(values, LimeXMLNames.TORRENT_TRACKERS, "http://localhost/announce http://test/comma,path");
        assertContainsNameValue(values, LimeXMLNames.TORRENT_LENGTH, "300");
        assertContainsNameValue(values, LimeXMLNames.TORRENT_NAME, "torrent file");
        assertContainsNameValue(values, LimeXMLNames.TORRENT_PRIVATE, "true");
        assertContainsNameValue(values, LimeXMLNames.TORRENT_WEBSEEDS, "http://localhost/name%20hello http://test/%22me%22");
        assertContainsNameValue(values, LimeXMLNames.TORRENT_FILE_PATHS, "/home/test me \"hello world\"///home/me/\u30d5\u30a1");
        assertContainsNameValue(values, LimeXMLNames.TORRENT_FILE_SIZES, "200 100");
    }
    
    private void assertContainsNameValue(List<NameValue<String>> values, String name,
            String value) {
        NameValue<String> nameValue = new NameValue<String>(name, value);
        for (NameValue<String> elem : values) {
            if (elem.getName().equals(name)) {
                if (elem.getValue().equals(value)) {
                    return;
                } else {
                    fail(nameValue + " not found, but " + elem);
                }
            }
        }
        fail(nameValue + " not found in " + values);
    }

    private URI[] getUris() {
        try {
            return new URI[] { URIUtils.toURI("http://localhost/name hello"), URIUtils.toURI("http://test/\"me\"") };
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    
}
