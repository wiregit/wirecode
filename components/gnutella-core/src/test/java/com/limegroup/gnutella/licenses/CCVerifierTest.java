package com.limegroup.gnutella.licenses;

import java.io.*;
import java.util.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;

import org.apache.commons.httpclient.*;

public final class CCVerifierTest extends BaseTestCase {
    
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

	public CCVerifierTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(CCVerifierTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testParsingRDF() throws Exception {

	    Verifier vf = new TestVerifier(RDF_GOOD);
	    Callback c = new Callback();
	    assertFalse(c.completed);
	    assertFalse(vf.isVerified());
	    assertFalse(vf.isVerifying());
	    assertFalse(vf.isVerificationDone());
	    vf.verify(c);
	    Thread.sleep(500);
	    assertTrue(vf.isVerified());
	    assertTrue(vf.isVerificationDone());
	    assertTrue(c.completed);
	    
    }
    
    private static class Callback implements VerificationCallback {
        boolean completed = false;
        
        public void verificationCompleted(Verifier verifier) {
            completed = true;
        }
    }
    
    
    private static class TestVerifier extends CCVerifier {
        private final String page;
        
        TestVerifier(String page) {
            super("license text", LICENSE_URI);
            this.page = page;
        }
        
        protected String getBody() {
            return page;
        }
    }
}
            
