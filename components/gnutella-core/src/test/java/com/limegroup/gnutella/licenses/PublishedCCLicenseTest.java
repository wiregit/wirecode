package com.limegroup.gnutella.licenses;

import java.io.IOException;
import java.io.StringReader;

import javax.swing.text.html.HTMLEditorKit;

import junit.framework.Assert;
import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.relaxng.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

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

    /**
     * HTML parser runs in a separate thread and stores
     * the comment-embedded RDF here
     */
    private String rdf;
    
    /**
     * Tests that the verification RDF validates
     * against a RELAX-NG Schema
     */
    
    public void testVerifyRDFValidates() {

        String rdfEmbeddedComment = PublishedCCLicense
            .getRDFRepresentation( "TIMMAY", "TIMMAY SLEEPAY", "2005",
                                   "Timmay Drink Espresso",
                                   "urn:sha1:TIMMAYESPRESSOH32VV4A2LJUDNLPAJ6",
                                   CCConstants.ATTRIBUTION_SHARE_NON_COMMERCIAL );
        
        // first extract the RDF out of the html comment
        
        ParserGetter kit = new ParserGetter( );
        HTMLEditorKit.Parser htmlParser = kit.getParser( );
        
        HTMLEditorKit.ParserCallback callback = new CommentExtractor();
        
        StringReader reader = new StringReader( rdfEmbeddedComment );
        
        synchronized (rdf) {
			try {
				htmlParser.parse(reader, callback, true);
			} catch (IOException e) {
				Assert.fail(e.toString());
			}

			// wait for parser to finish
			boolean waiting = true;

			while (waiting) {
				try {
					rdf.wait();
					waiting = false;
				} catch (InterruptedException e) {
				}
			}
		}
        
        // now let's parse the RDF and validate it against the schema
        
        // set up ManekiNeko parser
        SAXParser parser = new SAXParser();

        try {
			parser.setFeature("http://xml.org/sax/features/namespaces", true);
	        parser.setFeature("http://xml.org/sax/features/validation", true);
	        parser.setProperty("http://cyberneko.org/xml/properties/relaxng/schema-location",
	                           "lib/xml/schemas/ccverificationrdf-schema.rnc");
	        parser.parse( new InputSource( new StringReader( rdf ) ) );
		} catch (SAXException e) {
			Assert.fail(e.toString());
		} catch (IOException e) {
			Assert.fail(e.toString());
		}
        

    }
    
    
    private class ParserGetter extends HTMLEditorKit {

		/**
		 * 
		 */
		private static final long serialVersionUID = -4065408030391493623L;

		// purely to make this method public
		public HTMLEditorKit.Parser getParser() {
			return super.getParser();
		}

	}
    
    private class CommentExtractor extends HTMLEditorKit.ParserCallback {
		public void handleComment(char[] data, int pos) {
			synchronized (rdf) {
				rdf = new String(data);
			}
		}

		public void flush() {
			synchronized (rdf) {
				rdf.notify();
			}
		}
	}
 
}
