package com.limegroup.gnutella.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.KeyValue;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class LimeXMLDocumentTest extends LimeTestCase {
            
	public LimeXMLDocumentTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LimeXMLDocumentTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    public static void testHashcode() throws Exception {
    	List<KeyValue<String, String>> map = new ArrayList<KeyValue<String, String>>();
    	map.add(new KeyValue<String, String>(LimeXMLNames.APPLICATION_NAME, "value"));
    	LimeXMLDocument doc1 = new LimeXMLDocument(map, LimeXMLNames.APPLICATION_SCHEMA);
    	LimeXMLDocument doc2 = new LimeXMLDocument(map, LimeXMLNames.APPLICATION_SCHEMA);
    	assertEquals(doc1, doc2);
    	assertEquals(doc1.hashCode(), doc2.hashCode());

    	doc1.initIdentifier(new File("file"));
    	assertEquals(doc1, doc2);
    	assertEquals(doc1.hashCode(), doc2.hashCode());
    }
    
}	
