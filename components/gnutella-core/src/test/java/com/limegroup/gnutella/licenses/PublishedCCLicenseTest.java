package com.limegroup.gnutella.licenses;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class PublishedCCLicenseTest extends BaseTestCase {
    
    /** Standard constructors */
    public PublishedCCLicenseTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CCLicenseTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    
}
