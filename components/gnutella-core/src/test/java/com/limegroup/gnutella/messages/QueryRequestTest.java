package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnType;
import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * This class tests the QueryRequest class with HUGE v0.94 extensions.
 */
public final class QueryRequestTest extends BaseTestCase {
    
    private final String XML_STRING =
           "<?xml version=\"1.0\"?>" +
            "<audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\">" +
            "<audio title=\"xml\" artist=\"bloat\"/></audios>";
	
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
                ByteArrayOutputStream ggepBytes = new ByteArrayOutputStream();
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

                Set curUrnTypeSet = qr.getRequestedUrnTypes();
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
		QueryRequest qr = null;
		try {
			qr = QueryRequest.createQuery("test", (byte)-1);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
		try {
			qr = QueryRequest.createQuery("test", (byte)8);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}

		try {
			qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)-1);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}
		try {
			qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)8);
			fail("should have rejected query");
		} catch(IllegalArgumentException e) {}

		qr = QueryRequest.createQuery("test", (byte)1);
		qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)1);
		qr = QueryRequest.createQuery("test", (byte)3);
		qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)3);

	}

	/**
	 * Tests to make sure that queries are still created correctly
	 * if some of the potential data is not filled in, such as
	 * XML vs. no XML, URN vs. no URN, etc.
	 */
	public void testStillAcceptedIfOnlyPartsAreEmpty() throws Exception {
		QueryRequest qr = null;

		//String is double null-terminated.
		byte[] payload = new byte[2+3];
		payload[2] = (byte)65;
		qr = QueryRequest.createNetworkQuery(
		    new byte[16], (byte)0, (byte)0, payload, Message.N_UNKNOWN);

		
		// now test everything empty but URN
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((short)2);
		baos.write(0); // null query
		baos.write(0); // first null
		
		// no encoding problems in english
		baos.write(HugeTestUtils.URNS[0].toString().getBytes()); 		
		baos.write(0); // last null

		qr = QueryRequest.createNetworkQuery(
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
	public void testThatNetworkQueriesWithURNsAreAccepted() throws Exception {
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
            } catch(BadPacketException e) {
                fail("should have accepted query!!");
            }
        }
    }


    public void testQueriesWithOnlyURNsAreAccepted() throws Exception {
        QueryRequest qr = QueryRequest.createQuery(HugeTestUtils.SHA1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.write(baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        QueryRequest qrRead = (QueryRequest) Message.read(bais);
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
                QueryRequest qr = 
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
			qrTest = (QueryRequest)Message.read(bais);
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

        // not firewalled, not wanting rich, and can't do FWTrans, just 10000000
        payload[0] = (byte) 0x80;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // firewalled, not wanting rich, and can't do FWTrans, just 11000000
        payload[0] = (byte) 0xC0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // not firewalled, wanting rich, can't do FWTrans, just 10100000
        payload[0] = (byte) 0xA0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // firewalled, wanting rich, can't do FWTrans, just 11100000
        payload[0] = (byte) 0xE0;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.canDoFirewalledTransfer());

        // not firewalled, not wanting rich, and can do FWTrans, just 10000010
        payload[0] = (byte) 0x82;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // firewalled, not wanting rich, and can do FWTrans, just 11000010
        payload[0] = (byte) 0xC2;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // not firewalled, wanting rich, can do FWTrans, just 10100010
        payload[0] = (byte) 0xA2;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // firewalled, wanting rich, can do FWTrans, just 11100010
        payload[0] = (byte) 0xE2;
        qr = QueryRequest.createNetworkQuery(
            GUID.makeGuid(), (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.canDoFirewalledTransfer());

        // now test out-of-band stuff
        // ---------------------------------------------------------
        InetAddress stanford = InetAddress.getByName("www.stanford.edu");
        byte[] stanfordGuid = GUID.makeAddressEncodedGuid(stanford.getAddress(),
                                                          6346);

        // firewalled, wanting rich, desiring out-of-band, can't do firewalled
        // transfer
        payload[0] = (byte) 0xE4;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, not wanting rich, desiring out-of-band, can't do
        // firewalled transfer
        payload[0] = (byte) 0xC4;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, wanting rich, desiring out-of-band, can't do
        // firewalled transfer
        payload[0] = (byte) 0xA4;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, not wanting rich, desiring out-of-band, can't do
        // firewalled transfer
        payload[0] = (byte) 0x84;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(!qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, wanting rich, desiring out-of-band, can do firewalled
        // transfer
        payload[0] = (byte) 0xE6;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // firewalled, not wanting rich, desiring out-of-band, can do
        // firewalled transfer
        payload[0] = (byte) 0xC6;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, wanting rich, desiring out-of-band, can do
        // firewalled transfer
        payload[0] = (byte) 0xA6;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
        assertEquals("IP's not equal!", stanford.getHostAddress(), 
                     qr.getReplyAddress());
        assertEquals("Port's not equal!", 6346, qr.getReplyPort());

        // not firewalled, not wanting rich, desiring out-of-band, can do
        // firewalled transfer
        payload[0] = (byte) 0x86;
        qr = QueryRequest.createNetworkQuery(
            stanfordGuid, (byte)0, (byte)0, payload, Message.N_UNKNOWN);
        assertTrue(!qr.isFirewalledSource());
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.desiresOutOfBandReplies());
        assertTrue(qr.canDoFirewalledTransfer());
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
    

    public void testWhatIsNewQuery() throws Exception {
        QueryRequest outQuery = null, inQuery = null;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;

        {
            outQuery = QueryRequest.createWhatIsNewQuery(GUID.makeGuid(), 
                                                         (byte)3);
            baos = new ByteArrayOutputStream();
            outQuery.write(baos);
            bais = new ByteArrayInputStream(baos.toByteArray());
            inQuery = (QueryRequest) Message.read(bais);
            assertTrue(inQuery.isWhatIsNewRequest());
            assertEquals(inQuery, outQuery);
        }

        {
            outQuery = QueryRequest.createWhatIsNewOOBQuery(GUID.makeGuid(), 
                                                         (byte)3);
            baos = new ByteArrayOutputStream();
            outQuery.write(baos);
            bais = new ByteArrayInputStream(baos.toByteArray());
            inQuery = (QueryRequest) Message.read(bais);
            assertTrue(inQuery.isWhatIsNewRequest());
            assertEquals(inQuery, outQuery);
        }

    }


    public void testMetaFlagConstructor() throws Exception {
        try {
        QueryRequest query = 
            new QueryRequest(GUID.makeGuid(), (byte)3, "whatever", "", null,
                             null, null, true, Message.N_TCP, false, 0, false, 
                             1);
            assertTrue(false);
        }
        catch (IllegalArgumentException yes) {}

        // when no flag is set
        testMetaFlag(0);

        int[] flags = new int[6];
        flags[0] = QueryRequest.AUDIO_MASK;
        flags[1] = QueryRequest.VIDEO_MASK;
        flags[2] = QueryRequest.DOC_MASK;
        flags[3] = QueryRequest.IMAGE_MASK;
        flags[4] = QueryRequest.WIN_PROG_MASK;
        flags[5] = QueryRequest.LIN_PROG_MASK;
        for (int i = 0; i < flags.length; i++) {
            testMetaFlag(0 | flags[i]);
            for (int j = 0; j < flags.length; j++) {
                if (j == i) continue;
                testMetaFlag(0 | flags[i] | flags[j]);
                for (int k = 0; k < flags.length; k++) {
                    if (k == j) continue;
                    testMetaFlag(0 | flags[i] | flags[j] | flags[k]);
                    for (int l = 0; l < flags.length; l++) {
                        if (l == k) continue;
                        testMetaFlag(0 | flags[i] | flags[j] | flags[k] |
                                     flags[l]);
                        for (int m = 0; m < flags.length; m++) {
                            if (m == l) continue;
                            testMetaFlag(0 | flags[i] | flags[j] | flags[k] |
                                         flags[l] | flags[m]);
                        }
                    }
                }
            }
        }

        try {
        QueryRequest query = 
            new QueryRequest(GUID.makeGuid(), (byte)3, "whatever", "", null,
                             null, null, true, Message.N_TCP, false, 0, false,
                             0 | flags[0] | flags[1] | flags[2] |  flags[3] | 
                             flags[4] | flags[5]);
            assertTrue(false);
        }
        catch (IllegalArgumentException yes) {}

    }

    private void testMetaFlag(int flag) throws Exception {
        try {
        QueryRequest query = 
            new QueryRequest(GUID.makeGuid(), (byte)3, "whatever", "", null,
                             null, null, true, Message.N_TCP, false, 0, false,
                             flag);
        if (flag == 0) assertTrue(query.desiresAll());
        if ((flag & QueryRequest.AUDIO_MASK) > 0)
            assertTrue(query.desiresAudio());
        if ((flag & QueryRequest.VIDEO_MASK) > 0)
            assertTrue(query.desiresVideo());
        if ((flag & QueryRequest.DOC_MASK) > 0)
            assertTrue(query.desiresDocuments());
        if ((flag & QueryRequest.IMAGE_MASK) > 0)
            assertTrue(query.desiresImages());
        if ((flag & QueryRequest.WIN_PROG_MASK) > 0)
            assertTrue(query.desiresWindowsPrograms());
        if ((flag & QueryRequest.LIN_PROG_MASK) > 0)
            assertTrue(query.desiresLinuxOSXPrograms());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        query.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        QueryRequest qr = (QueryRequest) Message.read(bais);
        if (flag == 0) assertTrue(query.desiresAll());
        if ((flag & QueryRequest.AUDIO_MASK) > 0)
            assertTrue(qr.desiresAudio());
        if ((flag & QueryRequest.VIDEO_MASK) > 0)
            assertTrue(qr.desiresVideo());
        if ((flag & QueryRequest.DOC_MASK) > 0)
            assertTrue(qr.desiresDocuments());
        if ((flag & QueryRequest.IMAGE_MASK) > 0)
            assertTrue(qr.desiresImages());
        if ((flag & QueryRequest.WIN_PROG_MASK) > 0)
            assertTrue(qr.desiresWindowsPrograms());
        if ((flag & QueryRequest.LIN_PROG_MASK) > 0)
            assertTrue(qr.desiresLinuxOSXPrograms());
        }
        catch (IllegalArgumentException no) {
            assertTrue(false);
        }
    }

    
    public void testMetaFlagConstructor2() throws Exception {
        {
            QueryRequest query = 
                QueryRequest.createQuery(GUID.makeGuid(), "whatever", "",
                                         MediaType.getAudioMediaType());
            
            assertTrue(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                QueryRequest.createOutOfBandQuery(GUID.makeGuid(), "whatever", 
                                                  "",
                                                  MediaType.getVideoMediaType());
            
            assertFalse(query.desiresAudio());
            assertTrue(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                QueryRequest.createQuery(GUID.makeGuid(), "whatever", "",
                                         MediaType.getImageMediaType());
            
            assertFalse(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertTrue(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                QueryRequest.createWhatIsNewQuery(GUID.makeGuid(), (byte) 2,
                                               MediaType.getDocumentMediaType());
            
            assertFalse(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertTrue(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertFalse(query.desiresWindowsPrograms());
            assertFalse(query.desiresLinuxOSXPrograms());
        }
        {
            QueryRequest query = 
                QueryRequest.createWhatIsNewOOBQuery(GUID.makeGuid(), (byte) 2,
                                               MediaType.getProgramMediaType());
            
            assertFalse(query.desiresAudio());
            assertFalse(query.desiresVideo());
            assertFalse(query.desiresDocuments());
            assertFalse(query.desiresImages());
            assertEquals(CommonUtils.isWindows(), 
                         query.desiresWindowsPrograms());
            assertEquals(CommonUtils.isLinux() || CommonUtils.isAnyMac(),
                         query.desiresLinuxOSXPrograms());
        }

    }

    public void testProxyQueries() throws Exception {
        QueryRequest query = QueryRequest.createQuery("sush");
        assertFalse(query.desiresOutOfBandReplies());
        QueryRequest proxy = QueryRequest.createProxyQuery(query,
                                                           query.getGUID());
        assertTrue(proxy.desiresOutOfBandReplies());
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
