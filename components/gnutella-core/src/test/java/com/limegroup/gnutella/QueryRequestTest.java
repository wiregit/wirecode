package com.limegroup.gnutella;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.util.StringTokenizer;
import junit.framework.*;
import junit.extensions.*;

/**
 * This class tests the QueryRequest class with HUGE v0.94 extensions.
 */
public final class QueryRequestTest extends TestCase {
	
	/**
	 * Constructs a new test instance for query requests.
	 */
	public QueryRequestTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(QueryRequestTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Contains the legacy unit test that was formerly in QueryReqest.
	 */
	public void testLegacyUnitTest() {
        int u2=0x0000FFFF;
        QueryRequest qr=new QueryRequest((byte)3,u2,"");
       
		assertEquals("min speeds should be equal", u2, qr.getMinSpeed());
		assertEquals("queries should be equal", "", qr.getQuery());

        qr=new QueryRequest((byte)3, 1,"ZZZ");		
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

        //String is empty.
        payload = new byte[2+1];
        qr = new QueryRequest(new byte[16], (byte)0, (byte)0, payload);
		assertEquals("queries should be equal", "", qr.getQuery());
	}


	/**
	 * Tests the constructor that most of the other constructors are built
	 * off of.
	 */
	public void testQueryRequestConstructorWithGUID() {
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
				assertEquals("query urn sets should be equal", curUrnSet, queryUrns);
				Set queryUrnTypes = qr.getRequestedUrnTypes();

				assertEquals("urn type set sizes should be equal", curUrnTypeSet.size(),
							 queryUrnTypes.size());
				assertEquals("urn types should be equal\r\n"+
							 "set 1: "+print(curUrnTypeSet)+"r\n"+
							 "set 2: "+print(queryUrnTypes), 
							 curUrnTypeSet,
							 queryUrnTypes);
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
		QueryRequest qr = new QueryRequest((byte)7, 0, query);
		String storedQuery = qr.getQuery();
		assertEquals("query strings should be equal", query, storedQuery);
		assertEquals("rich query should be the empty string", "", qr.getRichQuery());
		assertEquals("requested URN types should be the empty set", 
					 qr.getRequestedUrnTypes(), Collections.EMPTY_SET);
		assertEquals("URN Set should be the empty set", 
					 qr.getQueryUrns(), Collections.EMPTY_SET);
		assertEquals("min speeds don't match", 0, qr.getMinSpeed());

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
		QueryRequest qr = new QueryRequest(guid, ttl, minSpeed, query, richQuery, 
										   isRequery, requestedUrnTypes, queryUrns);
		
		assertEquals("ttls should be equal", ttl, qr.getTTL());
		assertEquals("min speeds should be equal", minSpeed, qr.getMinSpeed());
		assertEquals("queries should be equal", query, qr.getQuery());
		assertEquals("rich queries should be equal", richQuery, qr.getRichQuery());
		assertEquals("query urn types should be equal", requestedUrnTypes, 
					 qr.getRequestedUrnTypes());
		assertEquals("query urns should be equal", queryUrns, qr.getQueryUrns());
				
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			qr.write(baos);
			baos.flush();
		} catch (IOException e) {
			fail("unexpected exception: "+e);
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		QueryRequest qrTest = null;
		try {
			qrTest = (QueryRequest)qr.read(bais);
		} catch(Exception e) {
			fail("unexpected exception: "+e);
		}
		assertEquals("queries should be equal", qr.getQuery(), qrTest.getQuery());
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
		assertEquals("query urns should be equal", qr.getQueryUrns(), qrTest.getQueryUrns());
		assertEquals("min speeds should be equal", qr.getMinSpeed(), qrTest.getMinSpeed());
		
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
