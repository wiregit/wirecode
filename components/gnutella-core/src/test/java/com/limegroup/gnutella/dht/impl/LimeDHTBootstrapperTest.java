package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;

import junit.framework.Test;

import com.limegroup.gnutella.dht.DHTControllerStub;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;

public class LimeDHTBootstrapperTest extends BaseTestCase {
    
    public LimeDHTBootstrapperTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeDHTBootstrapperTest.class);
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
    
    public void testGetSimppHosts() throws Exception{
        LimeDHTBootstrapper bootstrapper = new LimeDHTBootstrapper(new DHTControllerStub());
        //set up SIMPP hosts
        String[] hosts = new String[] {
                "86.25.22.3:84","1.0.0.0.3:300"};
        
        DHTSettings.DHT_BOOTSTRAP_HOSTS.setValue(hosts);
        
        MojitoDHT dht = MojitoFactory.createDHT();
        PrivilegedAccessor.setValue(bootstrapper, "dht", dht);
        
        SocketAddress addr = bootstrapper.getSIMPPHost();
        
    }

}
