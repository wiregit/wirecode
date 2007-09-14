package com.limegroup.gnutella.gui;

import junit.framework.TestSuite;

import org.limewire.util.BaseTestCase;

public class GUIMediatorTest extends BaseTestCase {

    public GUIMediatorTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(GUIMediatorTest.class);
    }
    
}
