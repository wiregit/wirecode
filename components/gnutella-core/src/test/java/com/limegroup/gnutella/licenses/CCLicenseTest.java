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

    private static final URI LICENSE_URI;
    
    static {
        URI uri = null;
        try {
            uri = new URI("http://1.2.3.4/page".toCharArray());
        } catch(URIException muri) {
            uri = null;
        }
        LICENSE_URI = uri;
    }

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
	    License l = new TestLicense(RDF_GOOD);
	    Callback c = new Callback();
	    assertFalse(c.completed);
	    assertFalse(l.isVerified());
	    assertFalse(l.isVerifying());
	    l.verify(c);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertFalse(l.isValid(URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));	    
	    assertTrue(c.completed);
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed().toExternalForm());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription());
    }
    
    //public void testIsVerifying() throws Exception {
        // ADD
    //}
    
    public void testGuessLicenseDeed() throws Exception {
        License l = new TestLicense("", "");
        assertFalse(l.isVerified());
        assertNull(l.getLicenseDeed());
        
        l = new TestLicense("a license, http://creativecommons.org/licenses/mylicese is cool", "");
        assertFalse(l.isVerified());
        assertEquals("http://creativecommons.org/licenses/mylicese", l.getLicenseDeed().toExternalForm());
        
        l = new TestLicense("a license, http:// creativecommons.org/licenses/mylicese", "");
        assertFalse(l.isVerified());
        assertNull(l.getLicenseDeed());
        
        l = new TestLicense("http://creativecommons.org/licenses/me", "");
        assertEquals("http://creativecommons.org/licenses/me", l.getLicenseDeed().toExternalForm());
        
        l = new TestLicense("alazamhttp://creativecommons.org/licenses/me2", "");
        assertNull(l.getLicenseDeed());
        
        l = new TestLicense("http://limewire.org", "");
        assertNull(l.getLicenseDeed());
        
        l = new TestLicense("creativecommons.org/licenses", "");
        assertNull(l.getLicenseDeed());
    }
    
    public void testCopy() throws Exception {
        License l = new TestLicense("text1", "");
        assertEquals("text1", l.getLicense());
        
        License l2 = l.copy("text3");
        assertEquals("text3", l2.getLicense());
    }
    
    public void testSerializeAndDeserialize() throws Exception {
        License l = new TestLicense("license text", "");
        assertEquals("license text", l.getLicense());
        assertNull(l.getLicenseDeed());
        assertEquals("http://1.2.3.4/page", l.getLicenseURI().toString());
        assertEquals("Permissions unknown.", l.getLicenseDescription());
        assertFalse(l.isVerified());
        assertFalse(l.isValid(null));
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized
        assertNull(l.getLicenseDeed());
        assertEquals("http://1.2.3.4/page", l.getLicenseURI().toString());
        assertEquals("Permissions unknown.", l.getLicenseDescription());
        assertTrue(l.isVerified()); // CHANGE -- becomes verified
        assertFalse(l.isValid(null));
        
        // Now try with a full out parsed License.
	    l = new TestLicense("good license text", RDF_GOOD);
	    l.verify(null);
	    assertEquals("good license text", l.getLicense());
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed().toString());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription());
        
        bout = new ByteArrayOutputStream();
        out = new ObjectOutputStream(bout);
        out.writeObject(l);
        
        bin = new ByteArrayInputStream(bout.toByteArray());
        in = new ObjectInputStream(bin);
        l = (License)in.readObject();
        assertEquals(null, l.getLicense()); // CHANGE -- not serialized
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed().toString());
	    // more stringent than necessary - asserting order too.
	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
	                 "Prohibited: Fun\n" +
	                 "Required: Attribution, Notice", l.getLicenseDescription());
    }
    
    public void testAdvancedRDFParsing() throws Exception {
        // within HTML comments.
        License l = new TestLicense("<html><--" +
"<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" +
"   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" +
"   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
"  <Work/>" +
"  <License />" +
"</rdf:RDF>" +
"--></html>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals(null, l.getLicenseDeed());
	    assertEquals("Permissions unknown.", l.getLicenseDescription());
	    
	    
	    // No data.
	    l = new TestLicense("<rdf:RDF/>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));

        // RDF not bound.
	    l = new TestLicense("<rdf:RDF><Work/></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // Valid, no other info.
	    l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><Work/></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    
	    // Valid for specific URN.
	    l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work rdf:about=\"urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD\"/></rdf:RDF>");
	    l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    assertFalse(l.isValid(URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
	    
	    // No Work item.
        l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
        "<License/></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    
	    // No Work Item (but has a license deed)
        l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
        "<License rdf:about=\"http://mylicensedeed.com\"/></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertFalse(l.isValid(null));
	    assertEquals("http://mylicensedeed.com", l.getLicenseDeed().toExternalForm());
	    
	    // Valid, has license deed.
        l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
        "<Work/><License rdf:about=\"http://mylicensedeed.com\"/></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("http://mylicensedeed.com", l.getLicenseDeed().toExternalForm());
	    
	    // Valid, duplicate distribution permission ignored.
	    l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work/><License>" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"  </License></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Permitted: Distribution, DerivativeWorks\n" +
	                 "Required: Attribution", l.getLicenseDescription());
	    
        // Valid, unknown License element.
	    l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
	    "<Work/><License>" +
"     <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
"     <unknown rdf:resource=\"http://web.resource.org/cc/Unknown\" />" +
"     <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
"  </License></rdf:RDF>");
        l.verify(null);
	    assertTrue(l.isVerified());
	    assertTrue(l.isValid(null));
	    assertEquals("Permitted: Distribution, DerivativeWorks\n" +
	                 "Required: Attribution", l.getLicenseDescription());
	                 
        // Valid, unknown body element.
        l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
        "<Work/><Unknown/></rdf:RDF>");
        l.verify(null);
        assertTrue(l.isVerified());
        assertTrue(l.isValid(null));
        
        // Invalid -- Work is inside an unknown element.
        l = new TestLicense("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
        "<Unknown><Work/></Unknown></rdf:RDF>");
        l.verify(null);
        assertTrue(l.isVerified());
        assertFalse(l.isValid(null));
    }
    
    public void testHTTPRetrieval() throws Exception {
        TestBootstrapServer server = new TestBootstrapServer(20181);
        try {
            server.setResponseData("<html><head>Hi</head><body><--\n"+
                                   RDF_GOOD + "\n--></body></html>");
            
            License l = LicenseFactory.create("verify at http://127.0.0.1:20181/");
            l.verify(null);
            Thread.sleep(1000);
            assertTrue(l.isVerified());
    	    assertTrue(l.isValid(null));
    	    assertTrue(l.isValid(URN.createSHA1Urn("urn:sha1:MSMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));
    	    assertFalse(l.isValid(URN.createSHA1Urn("urn:sha1:SAMBC5VEUDLTC26UT5W7GZBAKZHCY2MD")));	    
    	    assertEquals("http://creativecommons.org/licenses/by/2.0/", l.getLicenseDeed().toExternalForm());
    	    // more stringent than necessary - asserting order too.
    	    assertEquals("Permitted: Reproduction, Distribution, DerivativeWorks\n" +
    	                 "Prohibited: Fun\n" +
    	                 "Required: Attribution, Notice", l.getLicenseDescription());
            assertEquals(1, server.getConnectionAttempts());
            assertEquals(1, server.getRequestAttempts());
        } finally {
            server.shutdown();
        }
    }
        
    
    private static class Callback implements VerificationListener {
        boolean completed = false;
        
        public void licenseVerified(License license) {
            completed = true;
        }
    }
    
    
    private static class TestLicense extends CCLicense {
        private final String page;
        
        TestLicense(String license, String page) {
            super(license, LICENSE_URI);
            this.page = page;
        }
        
        TestLicense(String page) {
            this("license text", page);
        }
        
        protected String getBody() {
            return page;
        }
        
        public void verify(VerificationListener listener) {
            super.verify(listener);
            try {
                Thread.sleep(500);
            } catch(InterruptedException ie) {
                fail(ie);
            }
        }
    }
}
            
