package com.limegroup.gnutella.connection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class GnutellaConnectionTest extends BaseTestCase {

    public GnutellaConnectionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GnutellaConnectionTest.class);
    }

    @Override
    protected void setUp() throws Exception {
    }

}
