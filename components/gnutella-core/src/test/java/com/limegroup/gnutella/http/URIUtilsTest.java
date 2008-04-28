package com.limegroup.gnutella.http;

import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class URIUtilsTest extends BaseTestCase {
    public URIUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
		return buildTestSuite(URIUtilsTest.class);
	}
    
    public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testURIDoesntNeedEncoding() throws URISyntaxException {
        assertEquals(new URI("http://www.foo.com"), URIUtils.toURI("http://www.foo.com"));    
    }
    
    public void testURINeedsEncoding() throws URISyntaxException {
        assertEquals(new URI("http://www.foo.com/my+file"), URIUtils.toURI("http://www.foo.com/my file"));  
    }
    
    public void testMalformedURI()  {
        try {
            URIUtils.toURI("htt%p://www.foo.com");  
            fail();
        } catch (URISyntaxException e) {
            // expected result
        }
    }
}
