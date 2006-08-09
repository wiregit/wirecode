package com.limegroup.gnutella.dht;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.BaseTestCase;

public class LimeDHTManagerTest extends BaseTestCase {
    
    
    public LimeDHTManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeDHTManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static void globalSetUp() throws Exception {
    }
    
    public static void globalTearDown() throws Exception {
    }
    
    protected void setUp() throws Exception {
    }

    public void tearDown() throws Exception{
        //Ensure no more threads.
        RouterService.shutdown();
    }
    
}