package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import junit.framework.*;
import junit.extensions.*;

/**
 * This class tests the QueryReply class.
 */
public final class QueryReplyTest extends com.limegroup.gnutella.util.BaseTestCase {

    private QueryReply.GGEPUtil _ggepUtil = new QueryReply.GGEPUtil();
	
	/**
	 * Constructs a new test instance for query replies.
	 */
	public QueryReplyTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryReplyTest.class);
	}

	/**
	 * Runs the test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Runs the legacy unit test that was formerly in QueryReply.
	 */
	public void testLegacy() throws Exception {		
		byte[] ip={(byte)0xFF, (byte)0, (byte)0, (byte)0x1};
		long u4=0x00000000FFFFFFFFl;
		byte[] guid=new byte[16]; guid[0]=(byte)1; guid[15]=(byte)0xFF;
		Response[] responses=new Response[0];
		QueryReply qr=new QueryReply(guid, (byte)5,
									 0xF3F1, ip, 1, responses,
									 guid, false);
		assertEquals(1, qr.getSpeed());
		assertEquals(Integer.toHexString(qr.getPort()), 0xF3F1, qr.getPort());

        assertEquals(qr.getResults().hasNext(), false);

		responses=new Response[2];
		responses[0]=new Response(11,22,"Sample.txt");
		responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
		qr=new QueryReply(guid, (byte)5,
						  0xFFFF, ip, u4, responses,
						  guid, false);
		assertEquals("255.0.0.1",qr.getIP());
		assertEquals(0xFFFF, qr.getPort());
		assertEquals(u4, qr.getSpeed());
		assertTrue(Arrays.equals(qr.getClientGUID(),guid));

		Iterator iter=qr.getResults();
		Response r1=(Response)iter.next();
		assertEquals(r1, responses[0]);
		Response r2=(Response)iter.next();
		assertEquals(r2, responses[1]);
		assertFalse(iter.hasNext());

		
		////////////////////  Contruct from Raw Bytes /////////////
		
 		//Normal case: double null-terminated result
		byte[] payload=new byte[11+11+16];
		payload[0]=1;            //Number of results
		payload[11+8]=(byte)65;  //The character 'A'

		qr=new QueryReply(new byte[16], (byte)5, (byte)0,
						  payload);


		iter=qr.getResults();
		Response response=(Response)iter.next();
		assertEquals("A", response.getName());
		assertFalse(iter.hasNext());

		try {
			qr.getVendor();    //undefined => exception
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
		try {
			qr.getNeedsPush(); //undefined => exception
			fail("qr should have been invalid");
		} catch (BadPacketException e) { }
		
		
        //Bad case: not enough space for client GUID.  We can get
        //the client GUID, but not the results.
        payload=new byte[11+11+15];
        payload[0]=1;                    //Number of results
        payload[11+8]=(byte)65;          //The character 'A'

		qr=new QueryReply(new byte[16], (byte)5, (byte)0,
						  payload);

        try {
            iter=qr.getResults();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getVendor();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }

        //Test case added by Sumeet Thadani to check the metadata part
        //Test case modified by Susheel Daswani to check the metadata part
        payload=new byte[11+11+(4+1+4+5)+16];
        payload[0]=1;                    //Number of results
        payload[11+8]=(byte)65;          //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1+2]=(byte)5; //size of xml lsb
        payload[11+11+4+1+3]=(byte)0; // size of xml msb
        payload[11+11+4+1+4]=(byte)'S';   //The character 'L'
        payload[11+11+4+1+4+1]=(byte)'U';   //The character 'L'
        payload[11+11+4+1+4+2]=(byte)'S';   //The character 'M'
        payload[11+11+4+1+4+3]=(byte)'H';   //The character 'E'
        payload[11+11+4+1+4+4]=(byte)0;   //null terminator
		

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        try {
            iter=qr.getResults();
            Response r = (Response)iter.next();
            assertEquals("sumeet test a", 1, r.getNameBytesSize());
            assertEquals("sumeet test b", 0, r.getMetaBytesSize());
            byte[] name = r.getNameBytes();
            assertEquals("sumeet test c", 'A', name[0]);
            assertEquals("Sumeet test1", "A",  r.getName());
            assertEquals("unexpected xml bytes",
						 "SUSH", (new String(qr.getXMLBytes())));
        }catch(BadPacketException e){
            fail("metaResponse not created well!", e);
        }

        //Normal case: basic metainfo with no vendor data
        payload=new byte[11+11+(4+1+4+4)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;  //The size of public area
        payload[11+11+4+1]=(byte)0xB1; // set push yes/no flag (and other stuff)
        payload[11+11+4+1+1]=(byte)0x01; // set the push understood flag
        payload[11+11+4+1+2]=(byte)4;  // set xml length
        payload[11+11+4+1+3]=(byte)0;

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        String vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertTrue(qr.getNeedsPush());
        
        //Normal case: basic metainfo with extra vendor data
        payload=new byte[11+11+(4+1+4+20000)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0xF0; //no push flag (and other crap)
        payload[11+11+4+1+1]=(byte)0x01; // push understood flag
        payload[11+11+4+1+2]=(byte)32; //size of xml lsb
        payload[11+11+4+1+3]=(byte)78; // size of xml msb
        for (int i = 0; i < 20000; i++)
            payload[11+11+4+1+4+i] = 'a';

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LLME", vendor);
        vendor=qr.getVendor();
        assertEquals(vendor, "LLME", vendor);
        assertFalse(qr.getNeedsPush());

        try {
            qr.getSupportsChat();
            fail("qr should have been invalid");
        } catch (BadPacketException e) {
        }

        //Weird case.  No common data.  (Don't allow.)
        payload=new byte[11+11+(4+1+2)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+4+1+0]=(byte)1;

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        try {
            qr.getNeedsPush();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try { 
            qr.getVendor();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }

        //Bad case.  Common payload length lies.
        payload=new byte[11+11+(4+2+0)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)2;

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        qr.getResults();
        
        try {
            qr.getVendor();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }  


        ///////////// BearShare 2.2.0 QHD (busy bits and friends) ///////////


        //Normal case: busy bit undefined and push bits unset.
        //(We don't bother testing undefined and set.  Who cares?)
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x0; //no data known
        payload[11+11+4+1+1]=(byte)0x0; 
        payload[11+11+4+1+2]=(byte)1;         

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);
						  
        vendor=qr.getVendor();
        assertEquals("unexpected vendor", "LIME", vendor);
        
        try {
            qr.getNeedsPush();
            fail("qr should have been invalid");
        } catch(BadPacketException e) {}
        try {
            qr.getIsBusy();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getHadSuccessfulUpload();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getIsMeasuredSpeed();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
       

        //Normal case: busy and push bits defined and set

        payload=new byte[11+11+(4+1+4+1+1)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)73;   //The character 'I'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;    //common payload size
        payload[11+11+4+1]=(byte)0x1d;    // 0001 1101
        payload[11+11+4+1+1]=(byte)0x1d;  // 0001 1101
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1
        payload[11+11+4+1+4]=(byte)0x1; //supports chat

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);
							  
        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertTrue(qr.getNeedsPush());
        assertTrue(qr.getIsBusy());
        assertTrue(qr.getIsMeasuredSpeed());
        assertTrue(qr.getHadSuccessfulUpload());
        assertTrue(qr.getSupportsChat());
          
        //Normal case: busy and push bits defined and unset
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x1c;    // 0001 1100
        payload[11+11+4+1+1]=(byte)0x01;  // 0000 0001  //push understood
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertFalse(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getIsMeasuredSpeed());
        assertFalse(qr.getHadSuccessfulUpload());

        try {
            qr.getSupportsChat();
            fail("LiME!=LIME when looking at private area");
        } catch (BadPacketException e) { }

        //Create extended QHD from scratch
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          false, true, true, false, true, false);
        assertEquals("255.0.0.1", qr.getIP());
        assertEquals(0xFFFF, qr.getPort());
        assertEquals(u4, qr.getSpeed());
        assertTrue(Arrays.equals(qr.getClientGUID(),guid));

        iter=qr.getResults();
        r1=(Response)iter.next();
        assertEquals(r1, responses[0]);
        r2=(Response)iter.next();
        assertEquals(r2, responses[1]);
        assertFalse(iter.hasNext());
        assertEquals("LIME", qr.getVendor());
        assertFalse(qr.getNeedsPush());
        assertTrue(qr.getIsBusy());
        assertTrue(qr.getHadSuccessfulUpload());
        assertFalse(qr.getIsMeasuredSpeed());
        assertTrue(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(!qr.isReplyToMulticastQuery());

        //Create extended QHD from scratch with different bits set
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          true, false, false, true, false, false);

        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(!qr.isReplyToMulticastQuery());

        //And check raw bytes....
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        qr.write(out);

        byte[] bytes=out.toByteArray();
        int ggepLen = _ggepUtil.getQRGGEP(true, false).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals((23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen, bytes.length);
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)Message.read(new ByteArrayInputStream(bytes));
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(!qr.isReplyToMulticastQuery());

        //Create extended QHD from scratch with different bits set
        // Do not set multicast, as that will unset pushing, busy, etc..
        // and generally confuse the test.
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          true, false, false, true, false, false);

        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, false).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(
            (23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen,
            bytes.length
        );
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)Message.read(new ByteArrayInputStream(bytes));
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());

        //Create from scratch with no bits set
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid, false);
        try {
            qr.getVendor();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getNeedsPush();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getIsBusy();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getHadSuccessfulUpload();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
        try {
            qr.getIsMeasuredSpeed();
            fail("qr should have been invalid");
        } catch (BadPacketException e) { }
	}

    public void testCalculateQualityOfService() {
        final byte[] addr=new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
        QueryReply reachableNonBusy=new QueryReply(
            new byte[16], (byte)5, 6346, addr, 0l, new Response[0], new byte[16],
            false, false,    //!needsPush, !isBusy
            true, false, false, false);
        QueryReply reachableBusy=new QueryReply(
            new byte[16], (byte)5, 6346, addr, 0l, new Response[0], new byte[16],
            false, true,     //!needsPush, isBusy
            true, false, false, false);
        QueryReply unreachableNonBusy=new QueryReply(
            new byte[16], (byte)5, 6346, addr, 0l, new Response[0], new byte[16],
            true, false,     //needsPush, !isBusy
            true, false, false, false);
        QueryReply unreachableBusy=new QueryReply(
            new byte[16], (byte)5, 6346, addr, 0l, new Response[0], new byte[16],
            true, true,      //needsPush, isBusy
            true, false, false, false);
        QueryReply oldStyle=new QueryReply(
            new byte[16], (byte)5, 6346, addr, 0l, new Response[0], 
            new byte[16], false);
        
        //Remember that a return value of N corresponds to N+1 stars
        assertEquals(3,  reachableNonBusy.calculateQualityOfService(false));
        assertEquals(3,  reachableNonBusy.calculateQualityOfService(true));
        assertEquals(1,  reachableBusy.calculateQualityOfService(false));
        assertEquals(1,  reachableBusy.calculateQualityOfService(true));
        assertEquals(2,  unreachableNonBusy.calculateQualityOfService(false));
        assertEquals(-1, unreachableNonBusy.calculateQualityOfService(true));
        assertEquals(0,  unreachableBusy.calculateQualityOfService(false));
        assertEquals(-1, unreachableBusy.calculateQualityOfService(true));        
        assertEquals(0,  oldStyle.calculateQualityOfService(false));
        assertEquals(0,  oldStyle.calculateQualityOfService(true));
    }


    public void testGGEPUtil() throws Exception {
        GGEP testGGEP = null;

        // test standard null GGEP....
        try {
            // this shouldn't even work....
            testGGEP = new GGEP(_ggepUtil.getQRGGEP(false, false), 0, null);
            assertTrue(false);
        }
        catch (BadGGEPBlockException expected) {}

        // test just BH GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, false), 0, null);
        assertEquals(1, testGGEP.getHeaders().size());
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(!testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

        // test just multicast GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(false, true), 0, null);
        assertEquals(1, testGGEP.getHeaders().size());
        assertTrue(!testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

        // test combo GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, true), 0, null);
        assertEquals(2, testGGEP.getHeaders().size());
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

    }

    /**
     * Test to make sure that results that have no name are rejected 
     */
    public void testThatEmptyResultsAreRejected() throws Exception {

 		// create a payload that says it has one result, but whose
        // result is empty.  This should be rejected!
		byte[] payload=new byte[11+11+16];
		payload[0] = 1;            //Number of results
		//payload[11+8]=(byte)65;  //The character 'A'

		QueryReply qr = 
            new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        
        try {
            List results = qr.getResultsAsList();
            fail("should have thrown an exception for empty result");
        } catch(BadPacketException e) {
        }
    }
    
}
