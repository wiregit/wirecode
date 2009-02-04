package org.limewire.core.impl.xmpp;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ApplicationServices;

public class XMPPResourceFactoryImplTest extends BaseTestCase  {
    
    public XMPPResourceFactoryImplTest(String name) {
        super(name);
    }
    
    /**
     * Test the return from getResource is
     *  non null, non empty, and equal
     *  across like invocations with the same guid
     */
    public void testGetResourceSane() {
        Mockery context = new Mockery();
        
        final ApplicationServices appServices = context.mock(ApplicationServices.class);
        
        XMPPResourceFactoryImpl factory = new XMPPResourceFactoryImpl(appServices);
        
        context.checking(new Expectations() {
            {
                // The guid should be looked up at least once
                //  or something strange is happening
                atLeast(2).of(appServices).getMyGUID();
                will(returnValue(new byte[] {1,2,3,4}));
                
            }});
        
       String resource1 = factory.getResource();
       String resource2 = factory.getResource();
       assertNotNull(resource1);
       assertNotNull(resource2);
       assertNotEquals("", resource1);
       assertNotEquals("", resource2);
       assertEquals("multiple invocations of getResource not equal", resource1, resource2);
       
       context.assertIsSatisfied();
    }
    
    /**
     * Test the return from getResource is
     *  non null, non empty, and unique
     *  across invocations with different guids
     */
    public void testGetResourceUniqueness() {
        Mockery context = new Mockery();
        
        final ApplicationServices appServices = context.mock(ApplicationServices.class);
        
        XMPPResourceFactoryImpl factory = new XMPPResourceFactoryImpl(appServices);
        
        context.checking(new Expectations() {
            {
                one(appServices).getMyGUID();
                will(returnValue(new byte[] {1,2,3,4}));
                one(appServices).getMyGUID();
                will(returnValue(new byte[] {4,2,3,1, 0xF}));

                
            }});
        
       String resource1 = factory.getResource();
       String resource2 = factory.getResource();
       assertNotNull(resource1);
       assertNotNull(resource2);
       assertNotEquals("", resource1);
       assertNotEquals("", resource2);
       assertNotEquals("multiple invocations of getResource with differnt hashes not unique", resource1, resource2);
       
       context.assertIsSatisfied();
    }
}
