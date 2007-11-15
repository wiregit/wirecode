package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.Collections;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IpPortImpl;
import org.limewire.rudp.UDPConnection;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.uploader.HTTPUploadSession;
import com.limegroup.gnutella.uploader.HTTPUploader;

public class FWTNodeInterceptorTest extends BaseTestCase {

    public FWTNodeInterceptorTest(String name) {
        super(name);
    }

    private Mockery context;

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    public void testProcess() throws Exception {
        final PushEndpointFactory pushEndpointFactory = context.mock(PushEndpointFactory.class);
        final PushEndpointCache pushEndpointCache = context.mock(PushEndpointCache.class);
        
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session); 
        FWTNodeInterceptor interceptor = new FWTNodeInterceptor(uploader, pushEndpointFactory);
        
        final PushEndpoint pe = new PushEndpoint(GUID.makeGuid(), Collections.singleton(new IpPortImpl("192.168.0.1:5555")), PushEndpoint.PLAIN, UDPConnection.VERSION, null, pushEndpointCache);
        
        // test success full construction
        context.checking(new Expectations() {{
            ignoring(pushEndpointCache);
            one(pushEndpointFactory).createPushEndpoint(with(new TypeSafeMatcher<String>() {
                // not using equal matcher, so we can evaluate lazily, due 
                // to circular dependency: PushEndPoint needs mocked cache to generate httpStringValue()
                @Override
                public boolean matchesSafely(String item) {
                    return item.equals(pe.httpStringValue());
                }
                public void describeTo(Description description) {
                }
                
            }));
            will(returnValue(pe));
        }});
        
        interceptor.process(HTTPHeaderName.FWT_NODE.create(pe.httpStringValue()), null);
        context.assertIsSatisfied();
        assertSame(pe, uploader.getPushEndpoint());
        
        // test exception being thrown in factory
        context.checking(new Expectations() {{
            one(pushEndpointFactory).createPushEndpoint(with(any(String.class)));
            will(throwException(new IOException()));
        }});
        uploader.setPushEndpoint(null);
        
        interceptor.process(HTTPHeaderName.FWT_NODE.create(pe.httpStringValue()), null);
        context.assertIsSatisfied();
        assertNull(uploader.getPushEndpoint());
    }

}
