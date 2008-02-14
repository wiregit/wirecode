package com.limegroup.bittorrent.tracking;

import junit.framework.Test;

import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

public class TrackerImplTest extends BaseTestCase {
    
    public TrackerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TrackerImplTest.class);
    }
    
    private Mockery mockery;
    public void setUp() throws Exception {
        mockery = new Mockery();
    }
    
    public void testConnectHTTP() throws Exception {
        
    }

}
