package com.limegroup.gnutella.dht.impl;

import junit.framework.Test;

public class AbstractDHTControllerTest extends DHTTestCase {

    public AbstractDHTControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AbstractDHTControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        setSettings();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
