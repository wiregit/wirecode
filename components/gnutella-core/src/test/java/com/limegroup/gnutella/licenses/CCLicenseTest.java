package com.limegroup.gnutella.licenses;

import java.io.*;
import java.util.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.bootstrap.*;

import org.apache.commons.httpclient.*;

public final class CCLicenseTest extends BaseTestCase {
    
    private static final String RDF_GOOD = 
"<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
"   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
"   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
"  <Work rdf:about=\"urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD\">" +
"     <dc:date>2003</dc:date>" +
"     <dc:format>audio/mpeg</dc:format>" +
"     <dc:identifier>http://example.com/mp3s/test.mp3</dc:identifier>" +
"     <dc:rights><Agent><dc:title>ExampleBand</dc:title></Agent></dc:rights>" +
"     <dc:title>Example Song</dc:title>" +
"     <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Sound\" />" +
"     <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />" +
"  </Work>" +
"  <License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"     <prohibits rdf:resource=\"http://web.resource.org/cc/Fun\"/>" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />" +
"  </License>" +
"</rdf:RDF>";

	public CCLicenseTest(String name) {
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
	
	public void testBasicParsingRDF() throws Exception {
	    URN good = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
	    URN bad = URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
	    
	    License l = new StubLicense(RDF_GOOD);
	    Callback c = new Callback();
	    assertFalse(c.completed);
	    assertFalse(l.isVerified());
	    assertFalse(l.isVerifying());
	    l.verify(c);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(good));
	    assertFalse(l.isValid(bad));
	    assertTrue(c.completed);
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toExternalForm());
        assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(good).toExternalForm());
        assertEquals(null, l.getLicenseDeed(bad));
	    // more stringent than necessary - asserting order too.
        String desc = "Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                  "Prohibited: Fun\n" +
	                  "Required: Attribution, Notice";
	    assertEquals(desc, l.getLicenseDescription(null));
	    assertEquals(desc, l.getLicenseDescription(good));
	    assertEquals("Permissions unknown.", l.getLicenseDescription(bad));
    }
    
    public void testGuessLicenseDeed() throws Exception {
        License l = new StubLicense("", "");
        assertFalse(l.isVerified());
        assertNull(l.getLicenseDeed(null));
        
        l = new StubLicense("a license, http://creativecommons.org/licenses/mylicese is cool", "");
        assertFalse(l.isVerified());
        assertEquals("http://creativecommons.org/licenses/mylicese", l.getLicenseDeed(null).toExternalForm());
        
        l = new StubLicense("a license, http:// creativecommons.org/licenses/mylicese", "");
        assertFalse(l.isVerified());
        assertNull(l.getLicenseDeed(null));
        
        l = new StubLicense("http://creativecommons.org/licenses/me", "");
        assertEquals("http://creativecommons.org/licenses/me", l.getLicenseDeed(null).toExternalForm());
        
        l = new StubLicense("alazamhttp://creativecommons.org/licenses/me2", "");
        assertNull(l.getLicenseDeed(null));
        
        l = new StubLicense("http://limewire.org", "");
        assertNull(l.getLicenseDeed(null));
        
        l = new StubLicense("creativecommons.org/licenses", "");
        assertNull(l.getLicenseDeed(null));
    }
    
    public void testCopy() throws Exception {
        License l = new StubLicense("text1", "");
        assertEquals("text1", l.getLicense());
        assertEquals("http://1.2.3.4/page", l.getLicenseURI().toString());
        
        License l2 = l.copy("text3", new URI("http://uri.com".toCharArray()));
        assertEquals("text3", l2.getLicense());
        assertEquals("http://uri.com", l2.getLicenseURI().toString());
    }
    
    public void testSerializeAndDeserialize() throws Exception {
        License l = new StubLicense("license text", "");
        assertEquals("license text", l.getLicense());
        assertNull(l.getLicenseDeed(null));
        assertEquals("http://1.2.3.4/page", l.getLicenseURI().toString());
        assertEquals("Permissions unknown.", l.getLicenseDescription(null));
        assertFalse(l.isVerified());
        assertFalse(l.isValid(null));
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized
        assertNull(l.getLicenseDeed(null));
        assertEquals(null, l.getLicenseURI()); // CHANGE -- not serialized
        assertEquals("Permissions unknown.", l.getLicenseDescription(null));
        assertTrue(l.isVerified()); // CHANGE -- becomes verified
        assertFalse(l.isValid(null));
        
        // Now try with a full out parsed License.
	    l = new StubLicense("good license text", RDF_GOOD);
	    l.verify(null);
	    assertEquals("good license text", l.getLicense());
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toString());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription(null));
        
        bout = new ByteArrayOutputStream();
        out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        bin = new ByteArrayInputStream(bout.toByteArray());
        in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toString());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription(null));
    }
    
    public void testAdvancedRDFParsing() throws Exception {
        URN goodUrn = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN goodUrn2 = URN.createSHA1Urn("urn:sha1:GOODC5VEUDLTC26UT5W7GZBAKZHCY2MD");
	    URN badUrn = URN.createSHA1Urn("urn:sha1:BADBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        
        // GOOD: within HTML comments.
        License l = new StubLicense("<html><--" +
"<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
"   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
"   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
"  <Work>" +
"    <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />" +
"  </Work>" +
"  <License />" +
"</rdf:RDF>" +
"--></html>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toString());
	    assertEquals("Permissions unknown.", l.getLicenseDescription(null));
	    
	    // BAD: No data.
	    l = new StubLicense("<rdf:RDF/>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));

        // BAD: RDF not bound.
	    l = new StubLicense("<rdf:RDF><Work/></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // BAD: Work, no license for it.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><Work/></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // BAD: Work, Valid for specific URN, but no license for it.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD\"/></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    assertFalse(l.isValid(goodUrn));
	    
	    // GOOD: Work, license.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work>" +
	    "<license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />"  +
	    "</Work></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertTrue(l.isValid(badUrn));
	    
	    // GOOD: Work, license & URN.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://mydeed.com\" />" +
	    "</Work></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertEquals("http://mydeed.com", l.getLicenseDeed(goodUrn).toExternalForm());
	    assertEquals("http://mydeed.com", l.getLicenseDeed(null).toExternalForm());
	    assertFalse(l.isValid(badUrn));
	    
	    // GOOD: Multiple works, licenses & URNs.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<Work rdf:about=\"" + goodUrn2 + "\">" +
	    "<license rdf:resource=\"http://deed2.com\" />" +
	    "</Work>" +
	    "</rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertTrue(l.isValid(goodUrn2));
	    assertEquals("http://deed1.com", l.getLicenseDeed(goodUrn).toExternalForm());
	    assertEquals("http://deed2.com", l.getLicenseDeed(goodUrn2).toExternalForm());
	    assertFalse(l.isValid(badUrn));
	    
	    // No Work item.
        l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
        "<License/></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // Valid, duplicate distribution permission ignored.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work>" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<License rdf:about=\"http://deed1.com\">" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"  </License></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Permitted: Distribution, DerivativeWorks\n" +
	                 "Required: Attribution", l.getLicenseDescription(null));
	    
        // Valid, unknown License element.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work>" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<License rdf:about=\"http://deed1.com\">" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <unknown rdf:resource=\"http://web.resource.org/cc/Unknown\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"  </License></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Permitted: Distribution, DerivativeWorks\n" +
	                 "Required: Attribution", l.getLicenseDescription(null));
	                 
        // Matches up the licenses among the multiple works correctly.
	    // GOOD: Multiple works, licenses & URNs.
	    l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"" + goodUrn + "\">" +
	    "<license rdf:resource=\"http://deed1.com\" />" +
	    "</Work>" +
	    "<Work rdf:about=\"" + goodUrn2 + "\">" +
	    "<license rdf:resource=\"http://deed2.com\" />" +
	    "</Work>" +
        "<License rdf:about=\"http://deed1.com\">" +
        "     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
        "     <prohibits rdf:resource=\"http://web.resource.org/cc/Evil\" />" +
        "  </License>" +
        "<License rdf:about=\"http://deed2.com\">" +
        "     <requires rdf:resource=\"http://web.resource.org/cc/Peace\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Love\" />" +
        "     <permits rdf:resource=\"http://web.resource.org/cc/Happiness\" />" +
        "  </License>" +     
	    "</rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(goodUrn));
	    assertTrue(l.isValid(goodUrn2));
	    assertEquals("http://deed1.com", l.getLicenseDeed(goodUrn).toExternalForm());
	    assertEquals("http://deed2.com", l.getLicenseDeed(goodUrn2).toExternalForm());
	    assertFalse(l.isValid(badUrn));
	    assertEquals("Permitted: Distribution\n" +
	                 "Prohibited: Evil\n" +
	                 "Required: Attribution", l.getLicenseDescription(goodUrn));
        assertEquals("Permitted: Love, Happiness\n" +
                     "Required: Peace", l.getLicenseDescription(goodUrn2));
	                 
        // Valid, unknown body element.
        l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
        "<Work><license rdf:resource=\"http://deed1.com\" /></Work>" +
        "<Unknown/></rdf:RDF>");
        l.verify(null);
        assertTrue(l.isVerified());
        assertTrue(l.isValid(null));
        
        // Invalid -- Work is inside an unknown element.
        l = new StubLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
        "<Unknown><Work><license rdf:resource=\"http://deed1.com\" /></Work></Unknown></rdf:RDF>");
        l.verify(null);
        assertTrue(l.isVerified());
        assertFalse(l.isValid(null));
    }
    
    public void testHTTPRetrieval() throws Exception {
        TestBootstrapServer server = new TestBootstrapServer(20181);
        URN good = URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        URN bad = URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD");
        try {
            server.setResponseData("<html><head>Hi</head><body><--\n"+
                                   RDF_GOOD + "\n--></body></html>");
            
            License l = LicenseFactory.create("verify at http://127.0.0.1:20181/");
            l.verify(null);
            Thread.sleep(1000);
            assertTrue(l.isVerified());
    	    assertTrue(l.isValid(null));
    	    assertTrue(l.isValid(good));
    	    assertFalse(l.isValid(bad));
    	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(good).toExternalForm());
    	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed(null).toExternalForm());
    	    assertEquals(null, l.getLicenseDeed(bad));
    	    // more stringent than necessary - asserting order too.
    	    String desc = "Permitted: Reproduction, Distribution, DerivativeWorks\n" +
    	                  "Prohibited: Fun\n" +
    	                  "Required: Attribution, Notice";
    	    assertEquals(desc, l.getLicenseDescription(good));
            assertEquals(desc, l.getLicenseDescription(null));
            assertEquals("Permissions unknown.", l.getLicenseDescription(bad));
            assertEquals(1, server.getConnectionAttempts());
            assertEquals(1, server.getRequestAttempts());
        } finally {
            server.shutdown();
        }
    }
}
            
