package com.limegroup.gnutella.uploader;

import junit.framework.Test;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.limewire.util.BaseTestCase;

public class BrowseRequestHandlerTest extends BaseTestCase {

    public static Test suite() {
        return buildTestSuite(BrowseRequestHandlerTest.class);
    }
    
    public void testShouldIncludeNMS1UrnsRecognizesHeader() {
        HttpGet get = new HttpGet("http://hello.world.omc/");
        get.addHeader(new BasicHeader("X-NMS1", "1"));
        assertTrue(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
        
        get.addHeader(new BasicHeader("foo", "bar"));
        assertTrue(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
        
        get = new HttpGet("http://mymymy.my/");
        get.addHeader(new BasicHeader("X-NMS1", "0"));
        assertFalse(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
    }
    
    public void testShouldIncludeNMS1UrnsRecognizesQuery() {
        HttpGet get = new HttpGet("http://hello.world.omc/?nms1=1");
        assertTrue(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
        
        get = new HttpGet("http://hello.world.omc/?nms1=0");
        assertFalse(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
        
        get = new HttpGet("http://localhost:4545/me%40you.com/?foo=bar&nms1=1");
        assertTrue(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
        
        get = new HttpGet("http://12.4.4.4:4545/");
        assertFalse(BrowseRequestHandler.shouldIncludeNMS1Urns(get));
    }

}
