package com.limegroup.gnutella.licenses;

import junit.framework.*;

/**
 * Tests that all the subclass licenses are constructable and have the correct 
 * semantics.
 */
public class CreativeCommonsLicensesTest 
    extends com.limegroup.gnutella.util.BaseTestCase {

    public CreativeCommonsLicensesTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CreativeCommonsLicensesTest.class);
    }  

    public void testLicenses() throws IllegalArgumentException {
        CreativeCommonsLicense ccl = null;

        {
            ccl = new AttributionLicense();
            assertTrue(ccl.requiresAttribution());
            assertTrue(!ccl.allowsNoncommercialUseOnly());
            assertTrue(!ccl.disallowsDerivativeWorks());
            assertTrue(!ccl.isShareAlike());
        }

        {
            ccl = new AttributionNoDerivsLicense();
            assertTrue(ccl.requiresAttribution());
            assertTrue(!ccl.allowsNoncommercialUseOnly());
            assertTrue(ccl.disallowsDerivativeWorks());
            assertTrue(!ccl.isShareAlike());
        }

        {
            ccl = new AttributionNoDerivsNonCommercialLicense();
            assertTrue(ccl.requiresAttribution());
            assertTrue(ccl.allowsNoncommercialUseOnly());
            assertTrue(ccl.disallowsDerivativeWorks());
            assertTrue(!ccl.isShareAlike());
        }

        {
            ccl = new AttributionNonCommercialLicense();
            assertTrue(ccl.requiresAttribution());
            assertTrue(ccl.allowsNoncommercialUseOnly());
            assertTrue(!ccl.disallowsDerivativeWorks());
            assertTrue(!ccl.isShareAlike());
        }

        {
            ccl = new AttributionNonCommercialShareAlikeLicense();
            assertTrue(ccl.requiresAttribution());
            assertTrue(ccl.allowsNoncommercialUseOnly());
            assertTrue(!ccl.disallowsDerivativeWorks());
            assertTrue(ccl.isShareAlike());
        }

        {
            ccl = new AttributionShareAlikeLicense();
            assertTrue(ccl.requiresAttribution());
            assertTrue(!ccl.allowsNoncommercialUseOnly());
            assertTrue(!ccl.disallowsDerivativeWorks());
            assertTrue(ccl.isShareAlike());
        }



    }
}
