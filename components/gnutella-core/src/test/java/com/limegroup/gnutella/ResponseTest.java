package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.ByteOrder;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * This class tests the Response class.
 */
@SuppressWarnings("unchecked")
public final class ResponseTest extends com.limegroup.gnutella.util.LimeTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public ResponseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ResponseTest.class);
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
	public void testLegacyResponseUnitTest() throws Exception {
        Response r = new Response(3,4096,"A.mp3");
        assertEquals("A.mp3", r.getName());
        assertNull(r.getDocument());

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
        String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"192\"></audio></audios>";
        String xml2 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Speech\" bitrate=\"150\"></audio></audios>";
        
        //create documents.
        LimeXMLDocument d1 = null;
        LimeXMLDocument d2 = null;
        d1 = new LimeXMLDocument(xml1);
        d2 = new LimeXMLDocument(xml2);
        Response ra = new Response(12,231,"def1.txt",d1);
        Response rb = new Response(13,232,"def2.txt",d2);
		assertEquals("problem with doc constructor", d1, ra.getDocument());
		assertEquals("problem with doc constructor", d2, rb.getDocument());
	}
	
	/**
	 * Tests a simple response can be created from stream (no extensions)
	 */
	public void testSimpleCreateFromStream() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    // write out closing null.
	    baos.write((byte)0);
	    // add on 16 bytes so it thinks a GUID is there.
	    baos.write( new byte[16] );
	    baos.flush();
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    for(int i = 0; i < 16; i++) // read the blank GUID stuff we added
	        assertEquals(0, in.read());
	    assertEquals("leftover input", -1, in.read());
    }
    
    /**
     * Tests multiple responses can be stacked together (no extensions)
     */
    public void testMultipleSimpleResponses() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    // RESPONSE 1.
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    // RESPONSE 2.
	    ByteOrder.int2leb(2181, baos);
	    ByteOrder.int2leb(1981, baos);
	    name = new byte[] { 's', '.', 'a', '.', 'b', 0 };
	    baos.write(name);	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    // add on 16 bytes so it thinks a GUID is there.
	    baos.write( new byte[16] );
	    baos.flush();
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    
	    r = Response.createFromStream(in);
	    assertEquals("wrong index", 2181, r.getIndex());
	    assertEquals("wrong size", 1981, r.getSize());
	    assertEquals("wrong name", "s.a.b", r.getName());
	    
	    for(int i = 0; i < 16; i++) // read the blank GUID stuff we added
	        assertEquals(0, in.read());
	    assertEquals("leftover input", -1, in.read());
    }
    
    /**
     * Tests a response can have some data between the nulls (garbage)
     */
    public void testGarbageExtension() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write("this is a garbage extension".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension",
	        "this is a garbage extension", new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());        
	}
	
	/**
	 * Tests multiple extensions (garbage) can be added with the delimiter.
	 */
	public void testMultipleGarbageExtensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write("this is a garbage extension".getBytes());
	    baos.write((byte)0x1c);
	    baos.write("this is another garbage extension".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension",
	        "this is a garbage extension\u001cthis is another garbage extension",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());        	    
    }

    /**
     * Tests that a URN can be read from a response.
     */
    public void testHUGEUrn() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    // add the sha1
	    baos.write(sha1.getBytes());
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension", sha1, new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    Set urns = new HashSet();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
	    urns.add(urn);
	    assertEquals("wrong urns", urns, r.getUrns());
    }
    
    /**
     * Tests that HUGE can work even if seperated with chunk extensions.
     */
    public void testHUGEUrnWithOtherExtensions() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    // add the sha1
	    baos.write("junkie junk".getBytes());
	    baos.write((byte)0x1c);
	    baos.write(sha1.getBytes());
	    baos.write((byte)0x1c);
	    baos.write("jankie jank".getBytes());
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
        assertEquals("wrong extension",
	        "junkie junk\u001c"+sha1+"\u001cjankie jank",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    Set urns = new HashSet();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
	    urns.add(urn);
	    assertEquals("wrong urns", urns, r.getUrns());
    }
    
    /**
     * Tests that we understand bearshare1-style metadata
     */
    public void testBearShare1Extensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write("192 kbps 160".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    assertEquals("wrong extension",
	        "192 kbps 160",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    LimeXMLDocument doc = r.getDocument();
	    assertNotNull("should have document", doc);	    
	    assertEquals("wrong name",
	        "sam", doc.getValue("audios__audio__title__"));
	    assertEquals("wrong bitrate",
	        "192", doc.getValue("audios__audio__bitrate__"));
        assertEquals("wrong length",
            "160", doc.getValue("audios__audio__seconds__"));
    }
    
    /**
     * Tests that we understand bearshare2-style metadata
     */
    public void testBearShare2Extensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 'm', 'y', 0 };
	    baos.write(name);
	    
	    baos.write("256kbps something 3528".getBytes());
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sammy", r.getName());
	    assertEquals("wrong extension",
	        "256kbps something 3528",
	        new String(r.getExtBytes()));
	    assertEquals("leftover input", -1, in.read());
	    
	    LimeXMLDocument doc = r.getDocument();
	    assertNotNull("should have document", doc);	    
	    assertEquals("wrong name",
	        "sammy", doc.getValue("audios__audio__title__"));
	    assertEquals("wrong bitrate",
	        "256", doc.getValue("audios__audio__bitrate__"));
        assertEquals("wrong length",
            "3528", doc.getValue("audios__audio__seconds__"));
    }
    
    /**
     * Tests that GGEP'd ALT extensions are read correctly.
     */
    public void testGGEPAltExtension() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    GGEP info = new GGEP();
	    // locations: 1.2.3.4:1, 4.3.2.1:2
	    byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0 };
	    info.put(GGEP.GGEP_HEADER_ALTS, alts);
	    info.write(baos);
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    assertEquals("wrong number of locations", 2, r.getLocations().size());
	    Set endpoints = new IpPortSet();
	    endpoints.add( new IpPortImpl("1.2.3.4", 1) );
	    endpoints.add( new IpPortImpl("4.3.2.1", 2) );
	    assertEquals("wrong alts", endpoints, r.getLocations());
	    assertEquals("should have no time", -1, r.getCreateTime());
    }
    
    /** Tests TLS is read in addition. */
    public void testReadGGEPAltWithTLSExtension() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        ByteOrder.int2leb(257, baos);
        ByteOrder.int2leb(1029, baos);
        byte[] name = new byte[] { 's', 'a', 'm', 0 };
        baos.write(name);
        
        GGEP info = new GGEP();
        // locations: 1.2.3.4:1, 4.3.2.1:2, 2.3.4.5:6, 5.4.3.2:7, 2.3.3.2:8
        byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0, 2, 3, 4, 5, 6, 0, 5, 4, 3, 2, 7, 0, 2, 3, 3, 2, 8, 0 };
        info.put(GGEP.GGEP_HEADER_ALTS, alts);
        byte[] tlsIdx = { (byte)0xC8 }; // 11001
        info.put(GGEP.GGEP_HEADER_ALTS_TLS, tlsIdx );        
        info.write(baos);
        
        // write out closing null.
        baos.write((byte)0);
        
        byte[] output = baos.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(output);
        Response r = Response.createFromStream(in);
        assertEquals("wrong index", 257, r.getIndex());
        assertEquals("wrong size", 1029, r.getSize());
        assertEquals("wrong name", "sam", r.getName());
        // too annoying to check extension was correct.
        assertEquals("leftover input", -1, in.read());
        
        assertEquals("wrong number of locations", 5, r.getLocations().size());
        Set tls = new IpPortSet();
        Set nonTLS = new IpPortSet();
        for(IpPort ipp : r.getLocations()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls.add(ipp);
            else
                nonTLS.add(ipp);
        }
        // TLS should be 3, nonTLS == 2
        assertEquals(3, tls.size());
        assertEquals(2, nonTLS.size());
        Set endpoints = new IpPortSet();
        endpoints.add( new IpPortImpl("1.2.3.4", 1) );
        endpoints.add( new IpPortImpl("4.3.2.1", 2) );
        endpoints.add( new IpPortImpl("2.3.3.2", 8) );
        assertEquals(endpoints, tls);
        
        endpoints.clear();
        endpoints.add( new IpPortImpl("2.3.4.5", 6) );
        endpoints.add( new IpPortImpl("5.4.3.2", 7) );
        assertEquals(endpoints, nonTLS);
        
        assertEquals("should have no time", -1, r.getCreateTime());
    }
    
    /**
     * Tests that the GGEP'd CT extension is read correctly.
     */
    public void testGGEPCreateTimeExtension() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    GGEP info = new GGEP();
	    long time = System.currentTimeMillis();
	    info.put(GGEP.GGEP_HEADER_CREATE_TIME, time / 1000);
	    info.write(baos);
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    time = time / 1000 * 1000; // we lose precision when sending.	    
	    assertEquals("wrong time", time, r.getCreateTime());
	    assertEquals("should have no locs", new HashSet(), r.getLocations());
    }
    
    /**
     * Test GGEP with multiple extensions.
     */
    public void testGGEPMultipleExtensions() throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    GGEP info = new GGEP();
	    long time = System.currentTimeMillis();
	    info.put(GGEP.GGEP_HEADER_CREATE_TIME, time / 1000);
	    // locations: 1.2.3.4:1, 4.3.2.1:2
	    byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0 };
	    info.put(GGEP.GGEP_HEADER_ALTS, alts);
	    info.write(baos);
	    
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    time = time / 1000 * 1000; // we lose precision when sending.	    
	    assertEquals("wrong time", time, r.getCreateTime());
	    
	    assertEquals("wrong number of locations", 2, r.getLocations().size());
	    Set endpoints = new IpPortSet();
	    endpoints.add( new IpPortImpl("1.2.3.4", 1) );
	    endpoints.add( new IpPortImpl("4.3.2.1", 2) );
	    assertEquals("wrong alts", endpoints, r.getLocations());
    }        
        
    /**
     * Tests that GGEP can be both before & after other extensions.
     */
    public void testGGEPWithOtherExtensions() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";        
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
	    ByteOrder.int2leb(257, baos);
	    ByteOrder.int2leb(1029, baos);
	    byte[] name = new byte[] { 's', 'a', 'm', 0 };
	    baos.write(name);
	    
	    baos.write(sha1.getBytes());
	    
	    baos.write((byte)0x1c);
	    
	    GGEP info = new GGEP();
	    // locations: 1.2.3.4:1, 4.3.2.1:2
	    byte[] alts = { 1, 2, 3, 4, 1, 0, 4, 3, 2, 1, 2, 0 };
	    info.put(GGEP.GGEP_HEADER_ALTS, alts);
	    info.write(baos);
	    
	    baos.write((byte)0x1c);
	    
	    baos.write("192 kbps 160".getBytes());	    
	    
	    // write out closing null.
	    baos.write((byte)0);
	    
	    byte[] output = baos.toByteArray();
	    ByteArrayInputStream in = new ByteArrayInputStream(output);
	    Response r = Response.createFromStream(in);
	    assertEquals("wrong index", 257, r.getIndex());
	    assertEquals("wrong size", 1029, r.getSize());
	    assertEquals("wrong name", "sam", r.getName());
	    // too annoying to check extension was correct.
	    assertEquals("leftover input", -1, in.read());
	    
	    assertEquals("wrong number of locations", 2, r.getLocations().size());
	    Set endpoints = new IpPortSet();
	    endpoints.add( new IpPortImpl("1.2.3.4", 1) );
	    endpoints.add( new IpPortImpl("4.3.2.1", 2) );
	    assertEquals("wrong alts", endpoints, r.getLocations());
	    
	    Set urns = new HashSet();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
	    urns.add(urn);
	    assertEquals("wrong urns", urns, r.getUrns());
	    
	    LimeXMLDocument doc = r.getDocument();
	    assertNotNull("should have document", doc);	    
	    assertEquals("wrong name",
	        "sam", doc.getValue("audios__audio__title__"));
	    assertEquals("wrong bitrate",
	        "192", doc.getValue("audios__audio__bitrate__"));
        assertEquals("wrong length",
            "160", doc.getValue("audios__audio__seconds__"));	    
    }
    
    /**
     * Tests the GGEPUtil.addGGEP method to correctly write
     * the correct number of alts out, in the correct format.
     */
    public void testOnly10AltsAreWritten() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
        
        // First create a bunch of alts.
        AlternateLocationCollection alc =
            AlternateLocationCollection.create(urn);
        for(int i = 0; i < 20; i++) {
            AlternateLocation al = AlternateLocation.create("1.2.3." + i + ":1", urn);
            alc.add(al);
        }
        
        // Then get them as endpoints (should remove the extra alts).
        Set endpoints = getAsIpPorts(alc);
        assertEquals("didn't filter out extras", 10, endpoints.size());
        
        // Add them to the output stream as a GGEP block.
        Response.GGEPContainer gc = new Response.GGEPContainer(endpoints, -1);
        addGGEP(baos, gc);
        
        // See if we can correctly read the GGEP block.
        baos.flush();
        byte[] output = baos.toByteArray();
        GGEP alt = new GGEP(output, 0, null);
        Set headers = alt.getHeaders();
        assertEquals("wrong size", 1, headers.size());
        assertEquals("wrong header", "ALT", headers.iterator().next());
        byte[] data = alt.getBytes("ALT");
        assertEquals("wrong data length", 60, data.length);
        for(int i = 0; i < data.length; i+=6) {
            assertEquals(1, data[i]);
            assertEquals(2, data[i+1]);
            assertEquals(3, data[i+2]);
            // data[i+3] is the one that changed, and because
            // iterators may return values in any order,
            // it doesn't matter what it is, so long as everything
            // else is correct.
            assertEquals(1, data[i+4]);
            assertEquals(0, data[i+5]);
        }
    }
    
    public void testAltsAndTLSAreWritten() throws Exception {
        final String sha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URN urn = URN.createSHA1UrnFromUriRes("/uri-res/N2R?" + sha1);
        
        // First create a bunch of alts.
        AlternateLocationCollection alc = AlternateLocationCollection.create(urn);
        for(int i = 0; i < 10; i++) {
            AlternateLocation al = AlternateLocation.create("1.2.3." + i + ":1", urn, i % 3 == 0);
            alc.add(al);
        }
        
        // Then get them as endpoints (should remove the extra alts).
        Set endpoints = getAsIpPorts(alc);
        assertEquals("didn't filter out extras", 10, endpoints.size());
        
        // Add them to the output stream as a GGEP block.
        Response.GGEPContainer gc = new Response.GGEPContainer(endpoints, -1);
        addGGEP(baos, gc);
        
        // See if we can correctly read the GGEP block.
        baos.flush();
        byte[] output = baos.toByteArray();
        GGEP alt = new GGEP(output, 0, null);
        Set headers = alt.getHeaders();
        assertEquals("wrong size", 2, headers.size());
        assertTrue("no alt!", headers.contains("ALT"));
        assertTrue("no tls!", headers.contains("ALT_TLS"));
        byte[] data = alt.getBytes("ALT");
        assertEquals("wrong data length", 60, data.length);
        for(int i = 0; i < data.length; i+=6) {
            assertEquals(1, data[i]);
            assertEquals(2, data[i+1]);
            assertEquals(3, data[i+2]);
            assertEquals((int)Math.floor(i / 6), data[i+3]);
            assertEquals(1, data[i+4]);
            assertEquals(0, data[i+5]);
        }
        data = alt.getBytes("ALT_TLS");
        assertEquals("wrong tls length", 2, data.length);
        assertEquals(new byte[] { (byte)0x92, (byte)0x40 }, data );
    }
    
    /**
     * Tests the GGEPUtil.getGGEP method.
     */
    public void testGGEPUtilGetGGEP() throws Exception {
        GGEP ggep = new GGEP();
        Response.GGEPContainer container;
        long ctime;
        Set locs;
        byte[] data;
        
        data = new byte[20]; // not % 6.
        ggep.put("ALT", data);
        locs = getGGEP(ggep).locations;
        assertNotNull("should never be null", locs);
        assertEquals("shouldn't have locs", 0, locs.size());
        
        data = new byte[18]; // multiple of 6, but all blank (invalid)
        ggep.put("ALT", data);
        locs = getGGEP(ggep).locations;
        assertNotNull("should never be null", locs);
        assertEquals("shouldn't have locs", 0, locs.size());

        // multiple of 6, but same        
        byte[] d1 = {1, 2, 3, 4, 1, 0, 1, 2, 3, 4, 1, 0};
        ggep.put("ALT", d1);
        locs = getGGEP(ggep).locations;
        assertNotNull("should never be null", locs);
        assertEquals("wrong number of locs", 1, locs.size());
        assertEquals("wrong endpoint", 0,
           IpPort.COMPARATOR.compare(new IpPortImpl("1.2.3.4:1"), (IpPort)locs.iterator().next()));

        // multiple of 6, diff            
        byte[] d2 = {1, 2, 3, 4, 1, 0, 1, 2, 3, 4, 2, 0};
        ggep.put("ALT", d2);
        locs = getGGEP(ggep).locations;
        assertNotNull("should never be null", locs);
        assertEquals("wrong number of locs", 2, locs.size());
        Set eps = new IpPortSet();
        eps.add( new IpPortImpl("1.2.3.4:1") );
        eps.add( new IpPortImpl("1.2.3.4:2") );
        assertEquals("wrong endpoints", eps, locs);
        
        // ctime.
        ggep = new GGEP();
        ggep.put("CT", 5341L);
        ctime = getGGEP(ggep).createTime;
        assertEquals(5341000, ctime);
        
        // alt & ctime.
        ggep.put("CT", 1243L);
        ggep.put("ALT", d2);
        container = getGGEP(ggep);
        assertNotNull(container);
        assertNotNull(container.locations);
        assertEquals(2, container.locations.size());
        assertEquals(eps, container.locations);
        assertEquals(1243000, container.createTime);
        
        // invalid alt, valid ctime
        ggep.put("ALT", new byte[0]);
        ggep.put("CT", 3214);
        container = getGGEP(ggep);
        assertEquals(0, container.locations.size());
        assertEquals(3214000, container.createTime);
        
        // invalid ctime, valid alt
        ggep.put("ALT", d2);
        // use 9 bytes (1 byte over the max for a long)
        ggep.put("CT", new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 } );
        container = getGGEP(ggep);
        assertEquals(2, container.locations.size());
        assertEquals(eps, container.locations);
        assertEquals(-1, container.createTime);
    }
    
    public void testHashCode() {
        Response r1 = new Response(0, 0, "name");
        Response r2 = new Response(0, 1, "name");
        assertNotEquals(r1.hashCode(), r2.hashCode());
        
        Response r3 = new Response(1, 0, "name");
        assertNotEquals(r1.hashCode(), r3.hashCode());
        assertNotEquals(r2.hashCode(), r3.hashCode());
        
        assertEquals(r1.hashCode(), new Response(0, 0, "name").hashCode());
        
        // max int values
        r1 = new Response(Integer.MAX_VALUE, Integer.MAX_VALUE, "name");
        r2 = new Response(0, Integer.MAX_VALUE, "name");
        assertNotEquals(r1.hashCode(), r2.hashCode());
    }
    
    private Set getAsIpPorts(AlternateLocationCollection col)
      throws Exception {
        return (Set)PrivilegedAccessor.invokeMethod(Response.class,
            "getAsIpPorts", new Object[] { col } );
    }
    
    private void addGGEP(OutputStream os, Response.GGEPContainer gc) throws Exception {
        Class c = PrivilegedAccessor.getClass(Response.class, "GGEPUtil");
        PrivilegedAccessor.invokeMethod(c, "addGGEP",
            new Object[] { os, gc },
            new Class[] { OutputStream.class, Response.GGEPContainer.class } );
    }
    
    private Response.GGEPContainer getGGEP(GGEP info) throws Exception {
        Class c = PrivilegedAccessor.getClass(Response.class, "GGEPUtil");
        return (Response.GGEPContainer)PrivilegedAccessor.invokeMethod(
            c, "getGGEP", new Object[] { info } );
    }    
}
