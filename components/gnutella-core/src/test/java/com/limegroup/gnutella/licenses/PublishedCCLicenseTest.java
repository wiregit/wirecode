package com.limegroup.gnutella.licenses;

import java.io.StringReader;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.Parser;

import junit.framework.Test;

import org.cyberneko.relaxng.parsers.SAXParser;
import org.xml.sax.InputSource;

import com.limegroup.gnutella.util.LimeTestCase;

public class PublishedCCLicenseTest extends LimeTestCase {
    
    /** Standard constructors */
    public PublishedCCLicenseTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PublishedCCLicenseTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * HTML parser may run in a separate thread
     */
    
    private String rdf;
    private Object rdfLock = new Object();
    
    /**
     * Tests that the verification RDF validates
     * against a RELAX-NG Schema
     */
    
    public void testVerifyRDFValidates() throws Exception {

		String rdfEmbeddedComment = PublishedCCLicense.getRDFRepresentation(
				"TIMMAY", "TIMMAY SLEEPAY", "2005", "Timmay Drink Espresso",
				"urn:sha1:TIMMAYESPRESSOH32VV4A2LJUDNLPAJ6",
				CCConstants.ATTRIBUTION_SHARE_NON_COMMERCIAL);

		// first extract the RDF out of the html comment

		StringReader reader = new StringReader(rdfEmbeddedComment);
		
        HTMLEditorKit.ParserCallback callback = new CommentExtractor();
        getHTMLEditorKitParser().parse(reader, callback, true);
        
		synchronized (rdfLock) {
			// wait for parser to finish
			while ( rdf == null ) {
				try {
					System.err.println("about to wait");
					rdfLock.wait();
				} catch (InterruptedException e) {
				}
			}		
		}
		

		// now let's parse the RDF and validate it against the schema

		// set up ManekiNeko parser
		SAXParser parser = new SAXParser();

		parser.setFeature("http://xml.org/sax/features/namespaces", true);
		parser.setFeature("http://xml.org/sax/features/validation", true);

        // bug in MinekiNeko prevents use of compact schema.  use regular schema instead
        //		parser.setFeature("http://cyberneko.org/xml/features/relaxng/compact-syntax", true);

		parser.setProperty(
				"http://cyberneko.org/xml/properties/relaxng/schema-location",
				"lib/xml/schemas/ccverificationrdf-schema.rng");
		parser.parse(new InputSource(new StringReader(rdf)));

	}
    
    /** 
     * Returns the HTML parser as used by HTMLEditorKit. We could use
     * HTMLEditorKit to get an instance of it but HTMLEditorKit instantiates
     * some java.awt.Cursor objects internally that causes some problems 
     * on OSX even in java.awt.headless mode!
     */
    private static Parser getHTMLEditorKitParser() {
        try {
            Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
            return (Parser) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private class CommentExtractor extends HTMLEditorKit.ParserCallback {
		public void handleComment(char[] data, int pos) {
//			System.err.println("about to parse Comment");
			synchronized (rdfLock) {
				rdf = new String(data);
				rdfLock.notify();
			}
//			System.err.println("finished parsing Comment");
		}

//		public void flush() throws BadLocationException {
//			System.err.println("flush called");
//			synchronized (rdfLock) {
//				rdfLock.notify();
//			}
//		}
	}
 
}
