package com.limegroup.gnutella.xml;

import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.Expand;


/**
 * Unit tests for LimeXMLSchemaRepository
 */
public class LimeXMLSchemaRepositoryTest extends LimeTestCase {
            
	public LimeXMLSchemaRepositoryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LimeXMLSchemaRepositoryTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() throws Exception {
	    Expand.expandFile(
            CommonUtils.getResourceFile("com/limegroup/gnutella/xml/xml.war"), 
            CommonUtils.getUserSettingsDir()
        );
    }
    
    /**
     * Tests that the getAvailableSchemaURIs function correctly returns
     * all availables URIs.
     *
     * Note that this test is stricter than necessary, relying on the order.
     * If any new xsd files are are added (or the URIs within the existing
     * ones change) then change the order of the checks.
     */
    public void testAvailableSchemaURIs() {
        String[] availableSchemas 
            = ProviderHacks.getLimeXMLSchemaRepository().getAvailableSchemaURIs();
            
        check( availableSchemas[0], "application");
        check( availableSchemas[1], "audio");
        check( availableSchemas[2], "document");
        check( availableSchemas[3], "image");
        check( availableSchemas[4], "video");
    }
    
    private static void check(String actual, String name) {
        String expected = "http://www.limewire.com/schemas/" + name + ".xsd";
        assertEquals(expected, actual);
    }        
}
