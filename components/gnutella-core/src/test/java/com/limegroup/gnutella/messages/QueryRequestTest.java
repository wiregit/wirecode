package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.connection.BIOMessageReader;
import com.limegroup.gnutella.guess.*; 
import com.limegroup.gnutella.settings.*; 
import com.sun.java.util.collections.*;
import java.io.*;
import junit.framework.*;
import java.net.InetAddress;

/**
 * This class tests the QueryRequest class with HUGE v0.94 extensions.
 */
public final class QueryRequestTest extends BaseTestCase {
    
    private final String XML_STRING =
           "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"xml\" artist=\"bloat\"></audio></audios>";
	
	/**
	 * Constructs a new test instance for query requests.
	 */
	public QueryRequestTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryRequestTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Tests the constructor that most of the other constructors are built
	 * off of.
	 */
	public void testQueriesFromNetworkWithGGEP() {
		try {
			ByteArrayOutputStream[] baos = 
			    new ByteArrayOutputStream[HugeTestUtils.URNS.length];
			for(int i=0; i<HugeTestUtils.URNS.length; i++) {
				baos[i] = new ByteArrayOutputStream();
				baos[i].write(0);
				baos[i].write(0);
				baos[i].write(HugeTestUtils.QUERY_STRINGS[i].getBytes());
				baos[i].write(0);
                // write the GGEP stuff
                byte[] bytes = new byte[4];
                (new Random()).nextBytes(bytes);
                QueryKey qk = QueryKey.getQueryKey(bytes, true);
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                qk.write(qkBytes);
                GGEP ggepBlock = new GGEP(false); // do COBS
                ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                              qkBytes.toByteArray());
                ggepBlock.write(baos[i]);
                baos[i].write(0x1c);
				baos[i].write(0);
				QueryRequest qr = null;
				try {
					qr = QueryRequest.createNetworkQuery(
					    GUID.makeGuid(), (byte)6, (byte)4, 
                        baos[i].toByteArray(), Message.N_UNKNOWN);
				} catch(BadPacketException e) {
                    e.printStackTrace();
					fail("should have accepted query: "+qr);
				}
				assertEquals("speeds should be equal", 0, qr.getMinSpeed());
				assertEquals("queries should be equal", 
							 HugeTestUtils.QUERY_STRINGS[i], qr.getQuery());
				Set queryUrns = qr.getQueryUrns();
                assertEquals("should not have any URNs", 0, queryUrns.size());

                assertEquals("query keys should be equal",
                             qk, qr.getQueryKey());
			}		   
		} catch(IOException e) {
			fail("unexpected exception: "+e);
		}
	}	

	/**
	 * Tests to make sure that queries with no query string, no xml, and no
	 * URNs are not accepted.
	 */
	public void testEmptyQueryNotAccepted() {
		QueryRequest qr = null;
		try {
			qr = QueryRequest.createQuery("");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}		

		try {
			qr = QueryRequest.createRequery("");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}		
		try {
			qr = QueryRequest.createQuery((String)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		
		try {
			qr = QueryRequest.createQuery((URN)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		
		try {
			qr = QueryRequest.createRequery((String)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		

		try {
			qr = QueryRequest.createRequery((URN)null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}		
		try {
			qr = QueryRequest.createRequery(null, (byte)3);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				


		try {
			qr = QueryRequest.createQuery(null, "", "");
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			qr = QueryRequest.createQuery(new byte[16], null, "");
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			qr = QueryRequest.createQuery(new byte[16], "", null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			qr = QueryRequest.createQuery(new byte[16], "", "");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}	
			
		try {
			qr = QueryRequest.createQuery(new byte[15], "test", "");
			fail("exception should have been thrown");
		} catch(IllegalArgumentException e) {}				

		try {
			qr = QueryRequest.createQuery("test", null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				

		try {
			qr = QueryRequest.createMulticastQuery(null);
			fail("exception should have been thrown");
		} catch(NullPointerException e) {}				


		try {
			//String is double null-terminated.
			byte[] payload = new byte[2+3];
			qr = QueryRequest.createNetworkQuery(
			    new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN );
			fail("exception should have been thrown");
		} catch(BadPacketException e) {
		}
	}

	/**
	 * Tests to make sure that invalid TTL arguments are not accepted,
	 * and that valid ones are.
	 */
	public void testTTLParameters() {
		try {
			QueryRequest.createQuery("test", (byte)-1);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
		try {
			QueryRequest.createQuery("test", (byte)8);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}

		try {
			QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)-1);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
		try {
			QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)8);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
    
        // these should all go through without any exceptions
		QueryRequest.createQuery("test", (byte)1);
		QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)1);
		QueryRequest.createQuery("test", (byte)3);
		QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)3);

	}

	/**
	 * Tests to make sure that queries are still created correctly
	 * if some of the potential data is not filled in, such as
	 * XML vs. no XML, URN vs. no URN, etc.
	 */
	public void testStillAcceptedIfOnlyPartsAreEmpty() throws Exception {
		//String is double null-terminated.
		byte[] payload = new byte[2+3];
		payload[2] = (byte)65;
		QueryRequest.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN);

		
		// now test everything empty but URN
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((short)2);
		baos.write(0); // null query
		baos.write(0); // first null
		
		// no encoding problems in english
		baos.write(HugeTestUtils.URNS[0].toString().getBytes()); 		
		baos.write(0); // last null

		QueryRequest.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN);
	}

	/**
	 * Contains the legacy unit test that was formerly in QueryReqest.
	 */
	public void testLegacyUnitTest() throws Exception {
        QueryRequest qr = null;
       
		qr = QueryRequest.createQuery("blah");
		assertEquals("queries should be equal", "blah", qr.getQuery());

		qr = QueryRequest.createQuery("ZZZ");
		assertEquals("queries should be equal", "ZZZ", qr.getQuery());

        //String is single null-terminated.
		byte[] payload = new byte[2+2];
		payload[2]=(byte)65;
		qr=QueryRequest.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN);
		
  		assertEquals("queries should be equal", "A", qr.getQuery());
  		assertEquals("query lengths should be equal", 4, qr.getLength());

        //String is double null-terminated.
        payload = new byte[2+3];
        payload[2]=(byte)65;
        qr = QueryRequest.createNetworkQuery(
            new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN);

		assertEquals("queries should be equal", "A", qr.getQuery());

		try {
			//String is empty.
			payload = new byte[2+1];
			qr = QueryRequest.createNetworkQuery(
			    new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN);
			fail("should not have accepted query");
			//assertEquals("queries should be equal", "", qr.getQuery());
		} catch(BadPacketException e) {
			// this is expected for an empty query
		}
	}


    /**
     * Tests to make sure that network queries with URNS are not accepted.
     */
	public void testThatNetworkQueriesWithURNsAreNotAccepted() throws Exception {
		ByteArrayOutputStream[] URN_BYTES = 
		    new ByteArrayOutputStream[HugeTestUtils.URNS.length];
		for(int i=0; i<HugeTestUtils.URNS.length; i++) {
			URN_BYTES[i] = new ByteArrayOutputStream();
			URN_BYTES[i].write(0);
			URN_BYTES[i].write(0);
			URN_BYTES[i].write(HugeTestUtils.QUERY_STRINGS[i].getBytes());
			URN_BYTES[i].write(0);
			Set curUrnSet = new HashSet();
			Set curUrnTypeSet = new HashSet();
			curUrnTypeSet.add(UrnType.ANY_TYPE_SET);
			curUrnTypeSet = Collections.unmodifiableSet(curUrnTypeSet);
			for(int j=i; j<HugeTestUtils.URNS.length; j++) {
				URN_BYTES[i].write(HugeTestUtils.URNS[j].toString().getBytes());
				curUrnSet.add(HugeTestUtils.URNS[j]);
				if((j+1) != HugeTestUtils.URNS.length) {
					URN_BYTES[i].write(0x1c);
				}
			}
			URN_BYTES[i].write(0);
        }        

        byte ttl = 4;
        byte hops = 2;

        for(int i=0; i<HugeTestUtils.URNS.length; i++) {
            try {
                QueryRequest qr = 
                    QueryRequest.createNetworkQuery(GUID.makeGuid(), ttl, hops, 
                                                    URN_BYTES[i].toByteArray(), 
                                                    Message.N_UNKNOWN);
                fail("should not have accepted query: "+qr);
            } catch(BadPacketException e) {
                // should be thrown for queries with URNS
            }
        }
    }


    /**
     * Test that network queries without URNs are accepted.
     */
	public void testThatWellFormedNetworkQueriesAreAccepted() throws Exception {
		ByteArrayOutputStream[] baos = 
		    new ByteArrayOutputStream[HugeTestUtils.QUERY_STRINGS.length];

        byte ttl = 4;
        byte hops = 2;
		for(int i=0; i<HugeTestUtils.QUERY_STRINGS.length; i++) {
			baos[i] = new ByteArrayOutputStream();
			baos[i].write(0);
			baos[i].write(0);
			baos[i].write(HugeTestUtils.QUERY_STRINGS[i].getBytes());
			baos[i].write(0);
			baos[i].write(0);
			QueryRequest qr = QueryRequest.createNetworkQuery(
			    GUID.makeGuid(), ttl, hops, 
			    baos[i].toByteArray(), Message.N_UNKNOWN);

            assertEquals("incorrect hops", hops, qr.getHops());
            assertEquals("incorrect ttl", ttl, qr.getTTL());
			assertEquals("speeds should be equal", 0, qr.getMinSpeed());
			assertEquals("queries should be equal", 
						 HugeTestUtils.QUERY_STRINGS[i], qr.getQuery());

            // we don't currently verify this -- should we accept queries
            // that don't ask for URNs??
			Set queryUrnTypes = qr.getRequestedUrnTypes();
        }
	}


    private static final String[] ILLEGAL_QUERIES = 
        new String[SearchSettings.ILLEGAL_CHARS.getValue().length];
    
    static {
        for(int i=0; i<ILLEGAL_QUERIES.length; i++) {
            ILLEGAL_QUERIES[i] = "test"+SearchSettings.ILLEGAL_CHARS.getValue()[i];
        }
    }

    /**
     * Test that network queries without URNs are accepted.
     */
	public void testThatNetworkQueriesWithIllegalCharsAreNotAccepted() throws Exception {
		ByteArrayOutputStream[] baos = 
		    new ByteArrayOutputStream[ILLEGAL_QUERIES.length];

        byte ttl = 4;
        byte hops = 2;
		for(int i=0; i<ILLEGAL_QUERIES.length; i++) {
			baos[i] = new ByteArrayOutputStream();
			baos[i].write(0);
			baos[i].write(0);
			baos[i].write(ILLEGAL_QUERIES[i].getBytes());
			baos[i].write(0);
			baos[i].write(0);
            try {
                QueryRequest.createNetworkQuery(GUID.makeGuid(), ttl, hops, 
                                                    baos[i].toByteArray(), 
                                                    Message.N_UNKNOWN);
                
                fail("should not have accepted illegal query chars");
            } catch(BadPacketException e) {
                // this should be thrown for illegal chars
            }
        }
	}
    


	/**
	 * Tests all constructors that take URNs.
	 */
	public void testUrnQueryConstructors() {
		Set urnSet = new HashSet();
		urnSet.add(HugeTestUtils.SHA1);
		QueryRequest qr = QueryRequest.createRequery(HugeTestUtils.SHA1);

		runStandardChecks(qr, qr.getQueryUrns());
		runRequeryChecks(qr);


		qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)4);
		runStandardChecks(qr, qr.getQueryUrns());
		runRequeryChecks(qr);
		assertEquals("TTLs should be equal", (byte)4, qr.getTTL());

		qr = QueryRequest.createQuery(HugeTestUtils.SHA1);
		runStandardChecks(qr, qr.getQueryUrns());
		runNonRequeryChecks(qr);
	}

	/**
	 * Tests constructor that only takes a string.
	 */
	public void testStringRequeryConstructor() {
		QueryRequest qr = QueryRequest.createRequery("test");
		runStandardChecks(qr, "test");
		runStringRequeryChecks(qr, "test");
	}


	/**
	 * Tests constructor that only takes a string.
	 */
	public void testStringConstructor() {
		QueryRequest qr = QueryRequest.createQuery("tests");
		runStandardChecks(qr, "tests");
		runNonRequeryChecks(qr);
	}

	/**
	 * Tests constructor that only takes a string and a TTL.
	 */
	public void testStringTTLConstructor() {
		QueryRequest qr = QueryRequest.createQuery("tests", (byte)3);
		runStandardChecks(qr, "tests");
		runNonRequeryChecks(qr);
		assertEquals("ttls should be equal", (byte)3, qr.getTTL());
	}

	/**
	 * Tests constructor that only takes a string and a TTL.
	 */
 	public void testStringXMLConstructor() throws Exception {
 		QueryRequest qr = QueryRequest.createQuery("tests", XML_STRING);
 		runStandardChecks(qr, "tests", XML_STRING);
 		runNonRequeryChecks(qr);
 	}

	/**
	 * Tests constructor that only takes a string and a TTL and a GUID.
	 */
	public void testGUIDStringXMLConstructor() throws Exception {
		QueryRequest qr = 
			QueryRequest.createQuery(QueryRequest.newQueryGUID(false),
			    "tests", XML_STRING);
		runStandardChecks(qr, "tests", XML_STRING);
		runNonRequeryChecks(qr);
	}

	/**
	 * Tests constructor that only takes a string and a query key.
	 */
	public void testStringQueryKeyConstructor() {
		QueryKey key = QueryKey.getQueryKey(GUID.makeGuid(), true);
		QueryRequest qr =
			QueryRequest.createQueryKeyQuery("test", key);

		runStandardChecks(qr, false, UrnType.ANY_TYPE_SET, 
						  Collections.EMPTY_SET, key);		
		assertEquals("unexpected query", "test", qr.getQuery());
		assertNull("unexpected xml query", qr.getRichQuery());
	}

	/**
	 * Tests constructor that only takes a QueryRequest for generating
	 * a multicast query.
	 */
	public void testMulticastQuery() {
		QueryRequest testQuery = QueryRequest.createQuery("test");
		QueryRequest qr = QueryRequest.createMulticastQuery(testQuery);		
		runStandardChecksMulticast(qr, "test");
	}

	/**
	 * Runs standard checks on requeries.
	 *
	 * @param qr the query to check
	 */
	private void runRequeryChecks(QueryRequest qr) {
		assertEquals("unexpected query", "\\", qr.getQuery());
		assertNull("xml should be empty", qr.getRichQuery());
		assertTrue("should have URNs", qr.hasQueryUrns());
		assertEquals("unexpected requested URN types", UrnType.SHA1_SET, 
					 qr.getRequestedUrnTypes());
		assertTrue("should be a requery", qr.isLimeRequery());
	}

	private void runStringRequeryChecks(QueryRequest qr, String query) {
		assertEquals("unexpected query", query, qr.getQuery());
		assertNull("xml should be null", qr.getRichQuery());
		assertTrue("should not have URNs", !qr.hasQueryUrns());
		assertEquals("unexpected requested URN types", UrnType.ANY_TYPE_SET, 
					 qr.getRequestedUrnTypes());
		assertTrue("should be a requery", qr.isLimeRequery());
	}

	private void runNonRequeryChecks(QueryRequest qr) {
		assertTrue("should not be a requery", !qr.isLimeRequery());		
	}

	/**
	 * Runs standard checks on query that should be valid for almost
	 * all queries.  This should not be 
	 *
	 * @param qr the query to check
	 */
	private void runStandardChecks(QueryRequest qr, 
								   boolean multicast, Set urnTypes,
								   Set urns, QueryKey key) {
		if(!multicast) {
			assertTrue("should not be multicast", !qr.isMulticast());
			assertTrue("should be firewalled", qr.isFirewalledSource());
		} else {
			assertTrue("should be multicast", qr.isMulticast());
			assertTrue("should not be firewalled", !qr.isFirewalledSource());
		}
		if(key == null) {
			assertNull("query key should be null", qr.getQueryKey());
		} else {
			assertEquals("unexpected query key", key, qr.getQueryKey());
		}
		assertTrue("should want XML", qr.desiresXMLResponses());
		assertEquals("unexpected requested URN types", urnTypes, 
					 qr.getRequestedUrnTypes());	
		assertEquals("URNs should be equal", urns, 
					 qr.getQueryUrns());	
        if(qr.getQueryUrns().size() == 0) {
            runIOChecks(qr);
        }
	}


	private void runStandardChecksMulticast(QueryRequest qr, String query) {
		assertEquals("queries should be equal", query, qr.getQuery());
		runStandardChecks(qr, true, UrnType.ANY_TYPE_SET,
						  Collections.EMPTY_SET, null);
	}

	private void runStandardChecks(QueryRequest qr) {
		runStandardChecks(qr, false, UrnType.ANY_TYPE_SET,
						  Collections.EMPTY_SET, null);
	}

	private void runStandardChecks(QueryRequest qr, Set urns) {
		runStandardChecks(qr, false, UrnType.SHA1_SET, urns, null);
	}
	
	/**
	 * Runs standard checks on query that should be valid for almost
	 * all queries.  This should not be 
	 *
	 * @param qr the query to check
	 * @param query the query string
	 */
	private void runStandardChecks(QueryRequest qr, String query) {
		assertEquals("queries should be equal", query, qr.getQuery());
		runStandardChecks(qr);
	}

	/**
	 * Runs standard checks on query that should be valid for almost
	 * all queries.  This should not be 
	 *
	 * @param qr the query to check
	 * @param query the query string
	 */
	private void runStandardChecks(QueryRequest qr, String query, String xml)
	  throws Exception {
	    if( xml == null || xml.equals("") )
	        assertNull("should not have xml", qr.getRichQuery());
	    else
		    assertEquals("xml queries should be equal", 
		        xml, qr.getRichQuery().getXMLString());
		runStandardChecks(qr);
	}

	/**
	 * Writes the specified query out to a stream and then reads the
	 * stream back in to make sure we end up with the same query we
	 * started with.
	 *
	 * @param qr the query to check
	 */
	private void runIOChecks(QueryRequest qr) {
		QueryRequest storedQuery = qr;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			qr.write(baos);
			baos.flush();
		} catch (IOException e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}

		ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());

		QueryRequest qrTest = null;
		try {
			qrTest = (QueryRequest)BIOMessageReader.read(bais);
		} catch(Exception e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}

		assertEquals("queries should be equal", storedQuery, qrTest);
	}


    public void testNewMinSpeedUse() throws Exception {
        
        QueryRequest qr = null;

        // BASIC FROM THE NETWORK TESTS
        // --------------------------------------
        //String is empty.
        byte[] payload = new byte[2+1];
        // these two don't matter....give a query string, otherwise the query
		// will be rejected
        payload[2] = (byte) 65;
        payload[1] = (byte) 0;

        // not firewalled and not wanting rich, just 10000000
        payload[0] = (byte) 0x80;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());

        // firewalled and not wanting rich, just 11000000
        payload[0] = (byte) 0xC0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());

        // not firewalled and wanting rich, just 10100000
        payload[0] = (byte) 0xA0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());

        // firewalled and wanting rich, just 11100000
        payload[0] = (byte) 0xE0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());

        // now test out-of-band stuff
        // ---------------------------------------------------------
        InetAddress stanford = InetAddress.getByName("www.stanford.edu");
        byte[] stanfordGuid = GUID.makeAddressEncodedGuid(stanford.getAddress(),
                                                          6346);

        // firewalled, wanting rich, desiring out-of-band - though as policy 
        // we never allow this - 11100100
        payload[0] = (byte) 0xE4;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, not wanting rich, desiring out-of-band - this can never
        // really happen because if you are firewalled how can you accept
        // out-of-band? - 11000100
        payload[0] = (byte) 0xC4;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, not wanting rich, desiring out-of-band - the only
        // case we care about. - 10000100
        payload[0] = (byte) 0x84;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // ---------------------------------------------------------

        
        // make sure that a multicast source overrides that firewalled bit.
        payload[0] = (byte) 0xE0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_MULTICAST);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.isMulticast());
        // --------------------------------------
    }

    public void testNetworkParameter() throws Exception {
        QueryRequest qr;
        
        qr = QueryRequest.createQuery("sam");
        assertTrue(!qr.isMulticast());
        assertEquals(Message.N_UNKNOWN, qr.getNetwork());
        
        qr = QueryRequest.createMulticastQuery(qr);
        assertTrue(qr.isMulticast());
        assertEquals(Message.N_MULTICAST, qr.getNetwork());
        
        //String is empty.
        byte[] payload = new byte[2+1];
        // these two don't matter....give a query string, otherwise the query
		// will be rejected
        payload[2] = (byte) 65;
        payload[1] = (byte) 0;
        
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isUnknownNetwork());
        assertEquals(Message.N_UNKNOWN, qr.getNetwork());
        
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_TCP);
        assertTrue(qr.isTCP());
        assertEquals(Message.N_TCP, qr.getNetwork());
        
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UDP);
        assertTrue(qr.isUDP());
        assertEquals(Message.N_UDP, qr.getNetwork());
        
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_MULTICAST);        
        assertTrue(qr.isMulticast());
        assertEquals(Message.N_MULTICAST, qr.getNetwork());
    }
    
    
	private static String print(Collection col) {
		Iterator iter = col.iterator();
		StringBuffer sb = new StringBuffer();
		while(iter.hasNext()) {
			sb.append(iter.next()); 
			sb.append("\r\n");
		}
		return sb.toString();
	}
}
