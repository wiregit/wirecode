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
		assertEquals(qr.getSpeed(), 1);
		assertEquals(Integer.toHexString(qr.getPort()), qr.getPort(), 0xF3F1);

        assertEquals(qr.getResults().hasNext(), false);

		responses=new Response[2];
		responses[0]=new Response(11,22,"Sample.txt");
		responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
		qr=new QueryReply(guid, (byte)5,
						  0xFFFF, ip, u4, responses,
						  guid, false);
		assertEquals(qr.getIP(), "255.0.0.1");
		assertEquals(qr.getPort(), 0xFFFF);
		assertEquals(qr.getSpeed(), u4);
		assertEquals(Arrays.equals(qr.getClientGUID(),guid), true);

		Iterator iter=qr.getResults();
		Response r1=(Response)iter.next();
		assertEquals(r1, responses[0]);
		Response r2=(Response)iter.next();
		assertEquals(r2, responses[1]);
		assertEquals(iter.hasNext(), false);

		
		////////////////////  Contruct from Raw Bytes /////////////
		
		//Normal case: double null-terminated result
		byte[] payload=new byte[11+11+16];
		payload[0]=1;            //Number of results
		payload[11+8]=(byte)65;  //The character 'A'

		qr=new QueryReply(new byte[16], (byte)5, (byte)0,
						  payload);


		iter=qr.getResults();
		Response response=(Response)iter.next();
		assertEquals("'"+response.getName()+"'", response.getName(), "A");
		assertEquals(iter.hasNext(), false);

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
            assertEquals("sumeet test a", r.getNameBytesSize(), 1);
            assertEquals("sumeet test b", r.getMetaBytesSize(), 0);
            byte[] name = r.getNameBytes();
            assertEquals("sumeet test c", name[0], 'A');
            assertEquals("Sumeet test1", r.getName(), "A");
            assertEquals("SUSH is not " + (new String(qr.getXMLBytes())), 
						 (new String(qr.getXMLBytes())), "SUSH");
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
        payload[11+11+4+1]=(byte)0xB1; //set push flag (and other stuff)
        payload[11+11+4+1+2]=(byte)4;  // set xml length
        payload[11+11+4+1+3]=(byte)0;

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        String vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertEquals(qr.getNeedsPush(), true);
        
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
        payload[11+11+4+1+2]=(byte)32; //size of xml lsb
        payload[11+11+4+1+3]=(byte)78; // size of xml msb
        for (int i = 0; i < 20000; i++)
            payload[11+11+4+1+4+i] = 'a';

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LLME", vendor);
        vendor=qr.getVendor();
        assertEquals(vendor, "LLME", vendor);
        assertEquals(qr.getNeedsPush(), false);

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
        assertEquals("Vendor should have been LIME", vendor, "LIME");
        
        assertTrue(!qr.getNeedsPush());
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
        payload[11+11+4+1]=(byte)0x1d;  //111X1 
        payload[11+11+4+1+1]=(byte)0x1c;  //111X0
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1
        payload[11+11+4+1+4]=(byte)0x1; //supports chat

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);
							  
        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertEquals(qr.getNeedsPush(), true);
        assertEquals(qr.getNeedsPush(), true);
        assertEquals(qr.getIsBusy(), true);
        assertEquals(qr.getIsBusy(), true);
        assertEquals(qr.getIsMeasuredSpeed(), true);
        assertEquals(qr.getIsMeasuredSpeed(), true);
        assertEquals(qr.getHadSuccessfulUpload(), true);
        assertEquals(qr.getHadSuccessfulUpload(), true);
        assertEquals(qr.getSupportsChat(), true);
          
        //Normal case: busy and push bits defined and unset
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x1c;  //111X1 
        payload[11+11+4+1+1]=(byte)0x0;  //111X0
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1

		qr=new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        vendor=qr.getVendor();
        assertEquals(vendor, "LIME", vendor);
        assertEquals(qr.getNeedsPush(), false);
        assertEquals(qr.getIsBusy(), false);
        assertEquals(qr.getIsMeasuredSpeed(), false);
        assertEquals(qr.getHadSuccessfulUpload(), false);

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
        assertEquals(qr.getIP(), "255.0.0.1");
        assertEquals(qr.getPort(), 0xFFFF);
        assertEquals(qr.getSpeed(), u4);
        assertTrue(Arrays.equals(qr.getClientGUID(),guid));

        iter=qr.getResults();
        r1=(Response)iter.next();
        assertEquals(r1, responses[0]);
        r2=(Response)iter.next();
        assertEquals(r2, responses[1]);
        assertEquals(iter.hasNext(), false);
        assertEquals(qr.getVendor(), "LIME");
        assertEquals(qr.getNeedsPush(), false);
        assertEquals(qr.getIsBusy(), true);
        assertEquals(qr.getHadSuccessfulUpload(), true);
        assertEquals(qr.getIsMeasuredSpeed(), false);
        assertEquals(qr.getSupportsChat(), true);
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

        assertEquals(qr.getVendor(), "LIME");
        assertEquals(qr.getNeedsPush(), true);
        assertEquals(qr.getIsBusy(), false);
        assertEquals(qr.getHadSuccessfulUpload(), false);
        assertEquals(qr.getIsMeasuredSpeed(), true);
        assertEquals(qr.getSupportsChat(), false);
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(!qr.isReplyToMulticastQuery());

        //And check raw bytes....
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        qr.write(out);

        byte[] bytes=out.toByteArray();
        int ggepLen = _ggepUtil.getQRGGEP(true, false).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(bytes.length,(23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen);
        assertEquals(bytes[bytes.length-16-6-ggepLen],0x3d); //11101
        assertEquals(bytes[bytes.length-16-5-ggepLen],0x31); //10001

        // check read back....
        qr=(QueryReply)Message.read(new ByteArrayInputStream(bytes));
        assertEquals(qr.getVendor(), "LIME");
        assertEquals(qr.getNeedsPush(), true);
        assertEquals(qr.getIsBusy(), false);
        assertEquals(qr.getHadSuccessfulUpload(), false);
        assertEquals(qr.getIsMeasuredSpeed(), true);
        assertEquals(qr.getSupportsChat(), false);
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(!qr.isReplyToMulticastQuery());

        //Create extended QHD from scratch with different bits set
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          true, false, false, true, false, true);

        assertEquals(qr.getVendor(), "LIME");
        assertEquals(qr.getNeedsPush(), true);
        assertEquals(qr.getIsBusy(), false);
        assertEquals(qr.getHadSuccessfulUpload(), false);
        assertEquals(qr.getIsMeasuredSpeed(), true);
        assertEquals(qr.getSupportsChat(), false);
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(qr.isReplyToMulticastQuery());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, true).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(bytes.length,(23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen);
        assertEquals(bytes[bytes.length-16-6-ggepLen],0x3d); //11101
        assertEquals(bytes[bytes.length-16-5-ggepLen],0x31); //10001

        // check read back....
        qr=(QueryReply)Message.read(new ByteArrayInputStream(bytes));
        assertEquals(qr.getVendor(), "LIME");
        assertEquals(qr.getNeedsPush(), true);
        assertEquals(qr.getIsBusy(), false);
        assertEquals(qr.getHadSuccessfulUpload(), false);
        assertEquals(qr.getIsMeasuredSpeed(), true);
        assertEquals(qr.getSupportsChat(), false);
        assertTrue(qr.getSupportsBrowseHost());
        assertTrue(qr.isReplyToMulticastQuery());

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
        assertTrue(testGGEP.getHeaders().size() == 1);
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(!testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

        // test just multicast GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(false, true), 0, null);
        assertTrue(testGGEP.getHeaders().size() == 1);
        assertTrue(!testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

        // test combo GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, true), 0, null);
        assertTrue(testGGEP.getHeaders().size() == 2);
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

    }
    
}
