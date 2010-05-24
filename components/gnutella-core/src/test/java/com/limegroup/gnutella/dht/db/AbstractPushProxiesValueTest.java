package com.limegroup.gnutella.dht.db;

import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.BitNumbers;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.BaseTestCase;



public class AbstractPushProxiesValueTest extends BaseTestCase {

    private Mockery context;

    public AbstractPushProxiesValueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AbstractPushProxiesValueTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    /**
     * Ensures different impls with same values are considered equal
     */
    public void testEqualsObject() throws Exception {
       final TestPushProxiesValue value 
           = new TestPushProxiesValue();
       
       final PushProxiesValue mockedValue 
           = context.mock(PushProxiesValue.class);
       
       context.checking(new Expectations() {{
           allowing(mockedValue).getTLSInfo();
           will(returnValue(value.getTLSInfo()));
           allowing(mockedValue).getPushProxies();
           will(returnValue(value.getPushProxies()));
           allowing(mockedValue).getGUID();
           will(returnValue(value.getGUID()));
           allowing(mockedValue).getFeatures();
           will(returnValue(value.getFeatures()));
           allowing(mockedValue).getFwtVersion();
           will(returnValue(value.getFwtVersion()));
           allowing(mockedValue).getPort();
           will(returnValue(value.getPort()));
           allowing(mockedValue).getVersion();
           will(returnValue(PushProxiesValue.VERSION));
       }});
           
       assertTrue(value.equals(mockedValue));
       
       context.assertIsSatisfied();
    }
    
    private static class TestPushProxiesValue extends AbstractPushProxiesValue {

        private GUID guid = new GUID();
        
        private IpPortSet proxies;
        
        public TestPushProxiesValue() throws Exception {
            super(PushProxiesValue.VERSION);
            proxies = new IpPortSet(new IpPortImpl("129.0.0.1", 9595));
        }
        
        @Override
        public byte getFeatures() {
            return 1;
        }

        @Override
        public int getFwtVersion() {
            return 5;
        }

        @Override
        public byte[] getGUID() {
            return guid.bytes();
        }

        @Override
        public int getPort() {
            return 6667;
        }

        @Override
        public Set<? extends IpPort> getPushProxies() {
            return proxies;
        }

        @Override
        public BitNumbers getTLSInfo() {
            return BitNumbers.EMPTY_BN;
        }
    }
}
