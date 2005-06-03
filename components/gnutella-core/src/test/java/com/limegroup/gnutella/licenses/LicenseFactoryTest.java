package com.limegroup.gnutella.licenses;

import java.io.*;
import java.util.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;

public final class LicenseFactoryTest extends BaseTestCase {

	public LicenseFactoryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LicenseFactoryTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testCreate() throws Exception {
	    License l = LicenseFactory.create(null);
	    assertNull(l);
	    
	    // BAD: no 'verify at'
	    l = LicenseFactory.create("no string");
	    assertNull(l);
	    
	    // CCBAD: 'verify at' without location
	    l = LicenseFactory.create("verify at");   
	    assertNotNull(l);
	    assertEquals(BadCCLicense.class, l.getClass());
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // CCBAD: 'verify at' with invalid URI
	    l = LicenseFactory.create("verify at nowhere");
        assertNotNull(l);
	    assertEquals(BadCCLicense.class, l.getClass());
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // a-ok CC
	    l = LicenseFactory.create("verify at http://home.org");
	    assertNotNull(l);
	    assertEquals(CCLicense.class, l.getClass());
	    assertFalse(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // BAD: no 'verify at'.
	    l = LicenseFactory.create("http://home.org");
	    assertNull(l);
	    
	    // CCBAD: no authority
	    l = LicenseFactory.create("verify at http://");
        assertNotNull(l);
	    assertEquals(BadCCLicense.class, l.getClass());
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // a-ok CC
	    l = LicenseFactory.create("verify at http://home.org/path");
	    assertNotNull(l);
	    assertEquals(CCLicense.class, l.getClass());
	    assertFalse(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // a-ok CC
	    l = LicenseFactory.create("this license should verify at http://nowhere.com");
	    assertNotNull(l);
	    assertEquals(CCLicense.class, l.getClass());
	    assertFalse(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // CCBAD: authority has spaces in it.
	    l = LicenseFactory.create("verify at http://life.org is not fair.");
        assertNotNull(l);
	    assertEquals(BadCCLicense.class, l.getClass());
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // 
	    
	    
    }
    
    public void testIsVerifiedAndValidAndCaching() throws Exception {
        URN urn1 = urn("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456");
        URN urn2 = urn("654321ZYXWVUTSRQPONMLKJIHGFEDBCA");
        URN urn3 = urn("ALPHABETSOUPMAKESGOODFOODYUMMMMY");
        URN urn4 = urn("BYEBYEMISSAMERICANPIEFROMTHELEVY");
        String rdf1 = rdf(urn1, true);
        String rdf2 = rdf(urn2, true);
        String rdf3 = rdf(urn3, false);
        String rdf4 = rdf(urn4, false);
        String vf1 = "verify at http://abc.com";
        String vf2 = "verify at http://def.com";
        String vf3 = "verify at http://ghi.com";
        String vf4 = "verify at http://jkl.com";
        
        // Make sure stuff starts off not being valid.
        assertFalse(LicenseFactory.isVerifiedAndValid(null, null));
        assertFalse(LicenseFactory.isVerifiedAndValid(null, ""));
        assertFalse(LicenseFactory.isVerifiedAndValid(urn1, null));
        assertFalse(LicenseFactory.isVerifiedAndValid(urn1, ""));
        assertFalse(LicenseFactory.isVerifiedAndValid(urn1, "verify at http://home.org"));
        assertFalse(LicenseFactory.isVerifiedAndValid(null, "verify at http://home.org"));
        
        // Okay, that's out of the way -- now cache some licenses and see if they're valid.
        License l1 = new StubCCLicense(vf1, rdf1);
        License l2 = new StubCCLicense(vf2, rdf2);
        License l3 = new StubCCLicense(vf3, rdf3);
        License l4 = new StubCCLicense(vf4, rdf4);
        l1.verify(null);
        l2.verify(null);
        l3.verify(null);
        l4.verify(null);
        
        // first do some sanity checks.
        assertTrue(l1.isValid(urn1));
        assertTrue(l2.isValid(urn2));
        assertFalse(l1.isValid(urn2));
        assertFalse(l2.isValid(urn1));
        assertFalse(l3.isValid(urn3));
        assertFalse(l4.isValid(urn4));
        assertTrue(l3.isValid(null));
        assertTrue(l4.isValid(null));
        
        // these are fine.
        assertTrue(LicenseFactory.isVerifiedAndValid(urn1, vf1));
        assertTrue(LicenseFactory.isVerifiedAndValid(urn2, vf2));
        
        // these are not.
        assertFalse(LicenseFactory.isVerifiedAndValid(urn3, vf3));
        assertFalse(LicenseFactory.isVerifiedAndValid(urn4, vf4));
        assertTrue(LicenseFactory.isVerifiedAndValid(null, vf3));
        assertTrue(LicenseFactory.isVerifiedAndValid(null, vf4));
        
        // check to make sure that verifying one urn against another page doesn't work
        assertFalse(LicenseFactory.isVerifiedAndValid(urn2, vf1));
        assertFalse(LicenseFactory.isVerifiedAndValid(urn1, vf2));
        
        // Now that 1, 2, 3, & 4 are verified & cached, retrieving the Licenses should
        // return already-verified & valid licenses.
        License d1 = LicenseFactory.create(vf1);
        License d2 = LicenseFactory.create(vf2);
        License d3 = LicenseFactory.create(vf3);
        License d4 = LicenseFactory.create(vf4);
        assertTrue(d1.isVerified());
        assertTrue(d2.isVerified());
        assertTrue(d3.isVerified());
        assertTrue(d4.isVerified());
        
        assertTrue(d1.isValid(urn1));
        assertTrue(d2.isValid(urn2));
        
        assertFalse(d3.isValid(urn3));
        assertFalse(d4.isValid(urn4));
        assertTrue(d3.isValid(null));
        assertTrue(d4.isValid(null));
    }
    
    
    private URN urn(String string) throws Exception {
        return URN.createSHA1Urn("urn:sha1:" + string);
    }
    
    private String rdf(URN urn, boolean valid) {
        if(valid) {
            return "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
                   "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
                   "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
                   "  <Work rdf:about=\"" + urn.httpStringValue() + "\">" +
                   "     <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />" +
                   "  </Work>" +
                   "</rdf:RDF>";
        } else {
            return "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
                   "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
                   "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
                   "  <Work rdf:about=\"" + 
                   urn.httpStringValue().substring(0, urn.httpStringValue().length()-1) + "X\">" +
                   "     <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />" +
                   "  </Work>" +
                   "</rdf:RDF>";
        }
    }
        
}
            
