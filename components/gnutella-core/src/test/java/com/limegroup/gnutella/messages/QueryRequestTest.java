package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.guess.*; 
import com.sun.java.util.collections.*;
import java.io.*;
import java.util.StringTokenizer;
import junit.framework.*;
import junit.extensions.*;

/**
 * This class tests the QueryRequest class with HUGE v0.94 extensions.
 */
public final class QueryRequestTest extends BaseTestCase {
	
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
			//String is double null-terminated.
			byte[] payload = new byte[2+3];
			qr = new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
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

	public void testStillAcceptedIfOnlyPartsAreEmpty() throws Exception {
		QueryRequest qr = null;
		qr = QueryRequest.createQuery("blah");
		qr = QueryRequest.createQuery("","blah");

		qr = QueryRequest.createRequery(HugeTestUtils.SHA1);

		//String is double null-terminated.
		byte[] payload = new byte[2+3];
		payload[2] = (byte)65;
		qr = new QueryRequest(new byte[16], (byte)0, (byte)0, payload);

		
		// now test everything empty but URN
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((short)2);
		baos.write(0); // null query
		baos.write(0); // first null
		
		// no encoding problems in english
		baos.write(HugeTestUtils.URNS[0].toString().getBytes()); 		
		baos.write(0); // last null

		qr = new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
	}

	/**
	 * Contains the legacy unit test that was formerly in QueryReqest.
	 */
	public void testLegacyUnitTest() throws Exception {
        int u2=0x0000FFFF;
        QueryRequest qr = null;
		try {
			qr = new QueryRequest((byte)3,u2,"", false);
			fail("should not have accepted empty query");
		} catch(IllegalArgumentException e) {
			// expected for empty query
		}
       
		qr = new QueryRequest((byte)3, u2, "blah", false);
		assertEquals("min speeds should be equal", u2, qr.getMinSpeed());
		assertEquals("queries should be equal", "blah", qr.getQuery());

        qr=new QueryRequest((byte)3, 1,"ZZZ", false);		
		assertEquals("min speeds should be equal", (byte)1, qr.getMinSpeed());
		assertEquals("queries should be equal", "ZZZ", qr.getQuery());

        //String is single null-terminated.
		byte[] payload = new byte[2+2];
		payload[2]=(byte)65;
		qr=new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
		
  		assertEquals("queries should be equal", "A", qr.getQuery());
  		assertEquals("query lengths should be equal", 4, qr.getLength());

        //String is double null-terminated.
        payload = new byte[2+3];
        payload[2]=(byte)65;
        qr = new QueryRequest(new byte[16], (byte)0, (byte)0, payload);

		assertEquals("queries should be equal", "A", qr.getQuery());

		try {
			//String is empty.
			payload = new byte[2+1];
			qr = new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
			fail("should not have accepted query");
			//assertEquals("queries should be equal", "", qr.getQuery());
		} catch(BadPacketException e) {
			// this is expected for an empty query
		}
	}


	/**
	 * Tests the constructor that most of the other constructors are built
	 * off of.
	 */
	public void testQueryRequestConstructorWithGUID1() throws Exception {
		ByteArrayOutputStream[] baos = 
		    new ByteArrayOutputStream[HugeTestUtils.URNS.length];
		for(int i=0; i<HugeTestUtils.URNS.length; i++) {
			baos[i] = new ByteArrayOutputStream();
			baos[i].write(0);
			baos[i].write(0);
			baos[i].write(HugeTestUtils.QUERY_STRINGS[i].getBytes());
			baos[i].write(0);
			Set curUrnSet = new HashSet();
			Set curUrnTypeSet = new HashSet();
			//curUrnTypeSet.add(UrnType.SHA1);
			curUrnTypeSet = Collections.unmodifiableSet(curUrnTypeSet);
			for(int j=i; j<HugeTestUtils.URNS.length; j++) {
				baos[i].write(HugeTestUtils.URNS[j].toString().getBytes());
				curUrnSet.add(HugeTestUtils.URNS[j]);
				if((j+1) != HugeTestUtils.URNS.length) {
					baos[i].write(0x1c);
				}
			}
			baos[i].write(0);
			QueryRequest qr = new QueryRequest(GUID.makeGuid(), (byte)6, 
											   (byte)4, 
											   baos[i].toByteArray());
			assertEquals("speeds should be equal", 0, qr.getMinSpeed());
			assertEquals("queries should be equal", 
						 HugeTestUtils.QUERY_STRINGS[i], qr.getQuery());
			Set queryUrns = qr.getQueryUrns();
			assertEquals("query urn sets should be equal", curUrnSet, 
                         queryUrns);
			Set queryUrnTypes = qr.getRequestedUrnTypes();

			assertEquals("urn type set sizes should be equal", 
                         curUrnTypeSet.size(),
						 queryUrnTypes.size());
			assertEquals("urn types should be equal\r\n"+
						 "set 1: "+print(curUrnTypeSet)+"r\n"+
						 "set 2: "+print(queryUrnTypes), 
						 curUrnTypeSet,
						 queryUrnTypes);
        }
	}	


	/**
	 * Tests the constructor that most of the other constructors are built
	 * off of.
	 */
	public void testQueryRequestConstructorWithGUID2() {
		try {
			ByteArrayOutputStream[] baos = 
			    new ByteArrayOutputStream[HugeTestUtils.URNS.length];
			for(int i=0; i<HugeTestUtils.URNS.length; i++) {
				baos[i] = new ByteArrayOutputStream();
				baos[i].write(0);
				baos[i].write(0);
				baos[i].write(HugeTestUtils.QUERY_STRINGS[i].getBytes());
				baos[i].write(0);
				Set curUrnSet = new HashSet();
				Set curUrnTypeSet = new HashSet();
				//curUrnTypeSet.add(UrnType.SHA1);
				curUrnTypeSet = Collections.unmodifiableSet(curUrnTypeSet);
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
                // write the URN stuff
				for(int j=i; j<HugeTestUtils.URNS.length; j++) {
					baos[i].write(HugeTestUtils.URNS[j].toString().getBytes());
					curUrnSet.add(HugeTestUtils.URNS[j]);
					if((j+1) != HugeTestUtils.URNS.length) {
						baos[i].write(0x1c);
					}
				}
				baos[i].write(0);
				QueryRequest qr = null;
				try {
					qr = new QueryRequest(GUID.makeGuid(), (byte)6, 
										  (byte)4, 
										  baos[i].toByteArray());
				} catch(BadPacketException e) {
					fail("should have accepted query: "+qr);
				}
				assertEquals("speeds should be equal", 0, qr.getMinSpeed());
				assertEquals("queries should be equal", 
							 HugeTestUtils.QUERY_STRINGS[i], qr.getQuery());
				Set queryUrns = qr.getQueryUrns();
				assertEquals("query urn sets should be equal", curUrnSet, 
                             queryUrns);
				Set queryUrnTypes = qr.getRequestedUrnTypes();

				assertEquals("urn type set sizes should be equal", 
                             curUrnTypeSet.size(),
							 queryUrnTypes.size());
				assertEquals("urn types should be equal\r\n"+
							 "set 1: "+print(curUrnTypeSet)+"r\n"+
							 "set 2: "+print(queryUrnTypes), 
							 curUrnTypeSet,
							 queryUrnTypes);
                assertTrue("query keys should be equal\nqk = " + qk + 
                           "\nqr = " + qr.getQueryKey(),
                           qk.equals(qr.getQueryKey()));
			}		   
		} catch(IOException e) {
			fail("unexpected exception: "+e);
		}
	}	


	/**
	 * Tests the constructor that only takes threee arguments.
	 */
	public void testQueryRequestThreeArgumentConstructor() {
		String query = "file";
		QueryRequest qr = new QueryRequest((byte)7, 0, query, false);
		String storedQuery = qr.getQuery();
		assertEquals("query strings should be equal", query, storedQuery);
		assertEquals("rich query should be the empty string", "", 
                     qr.getRichQuery());
		assertEquals("requested URN types should be the empty set", 
					 qr.getRequestedUrnTypes(), Collections.EMPTY_SET);
		assertEquals("URN Set should be the empty set", 
					 qr.getQueryUrns(), Collections.EMPTY_SET);
		assertEquals("min speeds don't match", 160, qr.getMinSpeed());

		try {
			Set queryUrns = qr.getQueryUrns();
			queryUrns.add(query);
			assertTrue("exception should have been thrown", true);
		} catch(com.sun.java.util.collections.UnsupportedOperationException e) {
		}

		try {
			Set requestedUrnTypes = qr.getRequestedUrnTypes();
			requestedUrnTypes.add(query);
			assertTrue("exception should have been thrown", true);
		} catch(com.sun.java.util.collections.UnsupportedOperationException e) {
		}
	}

	/**
	 * Tests the primary constructor that most of the other constructors
	 * delegate to.
	 */
	public void testQueryRequestSecondaryConstructor() {
		byte ttl = 5;
		int minSpeed = 30;
		String query = "file i really want";
		
		// ideally this would be a real rich query
		String richQuery = "";
		boolean isRequery = false;
		Set requestedUrnTypes = new HashSet();
		requestedUrnTypes.add(UrnType.SHA1);
		Set queryUrns = new HashSet();
		queryUrns.add(HugeTestUtils.URNS[4]);

		byte[] guid = QueryRequest.newQueryGUID(isRequery);
		QueryRequest qr = new QueryRequest(guid, ttl, minSpeed, query, 
                                           richQuery, isRequery, 
                                           requestedUrnTypes, queryUrns, false);
		
		assertEquals("ttls should be equal", ttl, qr.getTTL());
		assertEquals("min speeds should be equal", minSpeed, qr.getMinSpeed());
		assertEquals("queries should be equal", query, qr.getQuery());
		assertEquals("rich queries should be equal", richQuery, 
                     qr.getRichQuery());
		assertEquals("query urn types should be equal", requestedUrnTypes, 
					 qr.getRequestedUrnTypes());
		assertEquals("query urns should be equal", queryUrns, 
                     qr.getQueryUrns());
				
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			qr.write(baos);
			baos.flush();
		} catch (IOException e) {
			fail("unexpected exception: "+e);
		}

		ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());

		QueryRequest qrTest = null;
		try {
			qrTest = (QueryRequest)qr.read(bais);
		} catch(Exception e) {
			fail("unexpected exception: "+e);
		}
		assertEquals("queries should be equal", qr.getQuery(), 
                     qrTest.getQuery());
		assertEquals("rich queries should be equals", qr.getRichQuery(), 
					 qrTest.getRichQuery());

		Set urnTypes0 = qr.getRequestedUrnTypes();
		Set urnTypes1 = qrTest.getRequestedUrnTypes();
		Iterator iter0 = urnTypes0.iterator();
		Iterator iter1 = urnTypes1.iterator();
		UrnType urnType0 = (UrnType)iter0.next();
		UrnType urnType1 = (UrnType)iter1.next();
		assertEquals("urn types should both be sha1", urnType0, urnType1);
		assertEquals("urn type set sizes should be equal", urnTypes0.size(), 
					 urnTypes1.size());
		assertEquals("urn types should be equal\r\n"+
					 "set0: "+print(urnTypes0)+"\r\n"+
					 "set1: "+ print(urnTypes1), 
					 urnTypes0,
					 urnTypes1);
		assertEquals("query urns should be equal", qr.getQueryUrns(), 
                     qrTest.getQueryUrns());
		assertEquals("min speeds should be equal", qr.getMinSpeed(), 
                     qrTest.getMinSpeed());
		
	}


	/**
	 * Tests the primary constructor that most of the other constructors
	 * delegate to.
	 */
	public void testQueryRequestPrimaryConstructor() {
		byte ttl = 5;
		int minSpeed = 30;
		String query = "file i really want";
		
		// ideally this would be a real rich query
		String richQuery = "";
		boolean isRequery = false;
		Set requestedUrnTypes = new HashSet();
		requestedUrnTypes.add(UrnType.SHA1);
		Set queryUrns = new HashSet();
		queryUrns.add(HugeTestUtils.URNS[4]);

		byte[] guid = QueryRequest.newQueryGUID(isRequery);
        QueryKey qk = QueryKey.getQueryKey(guid, true);
		QueryRequest qr = new QueryRequest(guid, ttl, minSpeed, query, 
                                           richQuery, isRequery, 
                                           requestedUrnTypes, queryUrns, qk,
                                           false);
		
		assertEquals("ttls should be equal", ttl, qr.getTTL());
		assertEquals("min speeds should be equal", minSpeed, qr.getMinSpeed());
		assertEquals("queries should be equal", query, qr.getQuery());
		assertEquals("rich queries should be equal", richQuery, 
                     qr.getRichQuery());
		assertEquals("query urn types should be equal", requestedUrnTypes, 
					 qr.getRequestedUrnTypes());
		assertEquals("query urns should be equal", queryUrns, 
                     qr.getQueryUrns());
		assertEquals("query keys should be equal", qk, qr.getQueryKey());
				
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			qr.write(baos);
			baos.flush();
		} catch (IOException e) {
			fail("unexpected exception: "+e);
		}

		ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());

		QueryRequest qrTest = null;
		try {
			qrTest = (QueryRequest)qr.read(bais);
		} catch(Exception e) {
			fail("unexpected exception: "+e);
		}
		assertEquals("queries should be equal", qr.getQuery(), 
                     qrTest.getQuery());
		assertEquals("rich queries should be equals", qr.getRichQuery(), 
					 qrTest.getRichQuery());

		Set urnTypes0 = qr.getRequestedUrnTypes();
		Set urnTypes1 = qrTest.getRequestedUrnTypes();
		Iterator iter0 = urnTypes0.iterator();
		Iterator iter1 = urnTypes1.iterator();
		UrnType urnType0 = (UrnType)iter0.next();
		UrnType urnType1 = (UrnType)iter1.next();
		assertEquals("urn types should both be sha1", urnType0, urnType1);
		assertEquals("urn type set sizes should be equal", urnTypes0.size(), 
					 urnTypes1.size());
		assertEquals("urn types should be equal\r\n"+
					 "set0: "+print(urnTypes0)+"\r\n"+
					 "set1: "+ print(urnTypes1), 
					 urnTypes0,
					 urnTypes1);
		assertEquals("query urns should be equal", qr.getQueryUrns(), 
                     qrTest.getQueryUrns());
		assertEquals("min speeds should be equal", qr.getMinSpeed(), 
                     qrTest.getMinSpeed());
        assertTrue("qk = " + qk + " , read = " + qrTest.getQueryKey(), 
                   qk.equals(qrTest.getQueryKey()));
		
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
        qr = new QueryRequest(GUID.makeGuid(), (byte)0, (byte)0, payload);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());

        // firewalled and not wanting rich, just 11000000
        payload[0] = (byte) 0xC0;
        qr = new QueryRequest(GUID.makeGuid(), (byte)0, (byte)0, payload);
        assertTrue(!qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());

        // not firewalled and wanting rich, just 10100000
        payload[0] = (byte) 0xA0;
        qr = new QueryRequest(GUID.makeGuid(), (byte)0, (byte)0, payload);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());

        // firewalled and wanting rich, just 11100000
        payload[0] = (byte) 0xE0;
        qr = new QueryRequest(GUID.makeGuid(), (byte)0, (byte)0, payload);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        // --------------------------------------


        // CONSTRUCTION, WRITING TO A STREAM, READING FROM A STREAM
        // --------------------------------------
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        qr = new QueryRequest((byte)3, 0, "susheel", false);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(!qr.isFirewalledSource());
        try {
            baos = new ByteArrayOutputStream();
            qr.write(baos);
            bais = new ByteArrayInputStream(baos.toByteArray());
            QueryRequest check = (QueryRequest) Message.read(bais);
            assertTrue(check.desiresXMLResponses());
            assertTrue(!check.isFirewalledSource());
        }
        catch (Exception crap) {
            assertTrue(false);
        }
        
        qr = new QueryRequest((byte)3, 0, "susheel", true);
        assertTrue(qr.desiresXMLResponses());
        assertTrue(qr.isFirewalledSource());
        try {
            baos = new ByteArrayOutputStream();
            qr.write(baos);
            bais = new ByteArrayInputStream(baos.toByteArray());
            QueryRequest check = (QueryRequest) Message.read(bais);
            assertTrue(check.desiresXMLResponses());
            assertTrue(check.isFirewalledSource());
        }
        catch (Exception crap) {
            assertTrue(false);
        }

        { 
            // test rich use, but keep in mind that LimeWire's ALWAYS want rich
            // answers, so the bit should always be set....
            byte ttl = 5;
            int minSpeed = 0;
            String query = "file i really want";
            
            // ideally this would be a real rich query
            String richQuery = "something";
            boolean isRequery = false;
            Set requestedUrnTypes = new HashSet();
            requestedUrnTypes.add(UrnType.SHA1);
            Set queryUrns = new HashSet();
            queryUrns.add(HugeTestUtils.URNS[4]);
            
            byte[] guid = QueryRequest.newQueryGUID(isRequery);
            qr = new QueryRequest(guid, ttl, minSpeed, query, 
                                  richQuery, isRequery, 
                                  requestedUrnTypes, queryUrns,
                                  false);
            assertTrue(qr.desiresXMLResponses());
            assertTrue(!qr.isFirewalledSource());
            try {
                baos = new ByteArrayOutputStream();
                qr.write(baos);
                bais = new ByteArrayInputStream(baos.toByteArray());
                QueryRequest check = (QueryRequest) Message.read(bais);
                assertTrue(check.desiresXMLResponses());
                assertTrue(!check.isFirewalledSource());
            }
            catch (Exception crap) {
                assertTrue(false);
            }

            qr = new QueryRequest(guid, ttl, minSpeed, query, 
                                  richQuery, isRequery, 
                                  requestedUrnTypes, queryUrns,
                                  true);
            assertTrue(qr.desiresXMLResponses());
            assertTrue(qr.isFirewalledSource());
            try {
                baos = new ByteArrayOutputStream();
                qr.write(baos);
                bais = new ByteArrayInputStream(baos.toByteArray());
                QueryRequest check = (QueryRequest) Message.read(bais);
                assertTrue(check.desiresXMLResponses());
                assertTrue(check.isFirewalledSource());
            }
            catch (Exception crap) {
                assertTrue(false);
            }
        }
        // --------------------------------------
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
