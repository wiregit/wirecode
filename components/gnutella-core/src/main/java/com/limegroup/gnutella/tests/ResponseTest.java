package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.xml.*; 
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import junit.framework.*;
import junit.extensions.*;

/**
 * This class tests the Response class.
 */
public final class ResponseTest extends TestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public ResponseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(ResponseTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}


	/**
	 * Modified version of the unit test that was formerly embedded in
	 * the Response class.
	 */
	public void testLegacyResponseUnitTest() {
        Response r = new Response(3,4096,"A.mp3");
        int nameSize = r.getNameBytesSize();
		assertEquals(nameSize, 5);
        byte[] nameBytes = r.getNameBytes();
		assertEquals(65, nameBytes[0]);
		byte[] metaBytes = r.getMetaBytes();
		assertEquals("Spurios meta", 0, metaBytes.length);
		assertEquals("Meta size not right", 0, r.getMetaBytesSize());

        //
        //Response r2 = new Response("",999,4,"blah.txt");
		//assertEquals("bad meta", null, r2.getMetaBytes());
		//assertEquals("Meta size not right", 0, r2.getMetaBytesSize());
        //Assert.that(r2.getMetaBytes()==null,"bad meta");
        //Assert.that(r2.getMetaBytesSize() == 0,"Meta size not right");
        //String md = "Hello";
        //Response r3 = new Response(md,999,4,"king.txt");
		//assertEquals("bad meta", null, r3.getMetaBytes());
		//assertEquals("Meta size not right", 0, r3.getMetaBytesSize());
        //Assert.that(r3.getMetaBytes()==null,"bad meta");
        //Assert.that(r3.getMetaBytesSize() == 0,"Meta size not right");
        //The three formats we support
		/*
        String[] meta = {"a kbps 44.1 kHz b","akbps 44.1 kHz b", 
                                             "b akbps 44.1kHz" };
        for(int i=0;i<meta.length;i++){
            Response r4 = new Response(meta[i],999+i,4,"abc.txt");
            LimeXMLDocument d=null;
            String xml = r4.getMetadata();
            try{
                d = new LimeXMLDocument(xml);
            }catch (Exception e){
				assertTrue("XML not created well from between nulls", false);
				//Assert.that(false,"XML not created well from between nulls");
            }
            String br = d.getValue("audios__audio__bitrate__");
			assertEquals("a", br);
            //Assert.that(br.equals("a"));
            String len = d.getValue("audios__audio__seconds__");
			assertEquals("b", len);
            //Assert.that(len.equals("b"));
        }
		*/
        //Tests for checking new LimeXMLDocument code added.
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();

        String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"192\"></audio></audios>";
        
        String xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"150\"></audio></audios>";
        
        //create documents.
        LimeXMLDocument d1 = null;
        LimeXMLDocument d2 = null;
        try {
            d1 = new LimeXMLDocument(xml1);
            d2 = new LimeXMLDocument(xml2);
        } catch (Exception stop) {
			assertTrue("unexpected exception: "+stop+"\r\ntest failed", false);
            //System.out.println("Warning: Test is incorrect");
            //System.exit(1);
        }//not the Responses fault.
        Response ra = new Response(12,231,"def1.txt",d1);
        Response rb = new Response(13,232,"def2.txt",d2);
		assertEquals("problem with doc constructor", d1, ra.getDocument());
		assertEquals("problem with doc constructor", d2, rb.getDocument());
		assertEquals("mismatched strings"+ra.getMetadata()+", "+xml1, xml1, ra.getMetadata());
		assertEquals("mismatched strings"+rb.getMetadata()+", "+xml2, xml2, rb.getMetadata());
        //Assert.that(ra.getDocument() == d1, "problem with doc constructor");
        //Assert.that(rb.getDocument() == d2, "problem with doc constructor");
        
        //Assert.that(ra.getMetadata().equals(xml1),
		//          "mismatched strings"+ra.getMetadata()+", "+xml1);
        //Assert.that(rb.getMetadata().equals(xml2),
		//          "mismatched strings"+rb.getMetadata()+", "+xml2);
		
	}
}
