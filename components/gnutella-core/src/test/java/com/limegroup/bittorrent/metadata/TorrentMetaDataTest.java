package com.limegroup.bittorrent.metadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.http.URIUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.NameValue;
import org.limewire.util.StringUtils;

import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTData.BTFileData;

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
           allowing(data).getAnnounce();
           will(returnValue("http://localhost/announce"));
           allowing(data).getLength();
           will(returnValue(300L));
           allowing(data).getName();
           will(returnValue("torrent file"));
           allowing(data).getPieceLength();
           will(returnValue(2L));
           allowing(data).isPrivate();
           will(returnValue(false));
           allowing(data).getWebSeeds();
           will(returnValue(getUris()));
           allowing(data).getFiles();
           will(returnValue(Arrays.asList(new BTFileData(200L, "home/test me \"hello world\""), new BTFileData(100L, "home/me/\u30d5\u30a1"))));
        }});
        
        List<NameValue<String>> values = new TorrentMetaData(data).toNameValueList();
        assertContainsNameValue(values, "infohash", StringUtils.toUTF8String(infoHash));
        assertContainsNameValue(values, "announce", "http://localhost/announce");
        assertContainsNameValue(values, "length", "300");
        assertContainsNameValue(values, "name", "torrent file");
        assertContainsNameValue(values, "piecelength", "2");
        assertContainsNameValue(values, "private", "false");
        assertContainsNameValue(values, "webseeds", "http://localhost/name%09hello\thttp://test/%22me%22");
        assertContainsNameValue(values, "filepaths", "file:/home/test+me+%22hello+world%22\tfile:/home/me/%E3%83%95%E3%82%A1");
        assertContainsNameValue(values, "filelenghts", "200\t100");
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
            return new URI[] { URIUtils.toURI("http://localhost/name\thello"), URIUtils.toURI("http://test/\"me\"") };
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    
}
