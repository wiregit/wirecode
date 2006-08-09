package com.limegroup.gnutella.dht;

import junit.framework.Test;

import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.BaseTestCase;

public class DHTNodeFetcherTest extends BaseTestCase {

    public DHTNodeFetcherTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTNodeFetcherTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        DHTSettings.DHT_NODE_FETCHER_TIME.setValue(1000L);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
}
