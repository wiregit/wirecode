package com.limegroup.gnutella.xml;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.util.Expand;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * Unit tests for LimeXMLSchemaRepository
 */
public class LimeXMLSchemaRepositoryTest extends BaseTestCase {
            
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
            = LimeXMLSchemaRepository.instance().getAvailableSchemaURIs();
            
        check( availableSchemas[0], "audio");
        check( availableSchemas[1], "video");
    }
    
    private static void check(String actual, String name) {
        String expected = "http://www.limewire.com/schemas/" + name + ".xsd";
        assertEquals(expected, actual);
    }        
}