package com.limegroup.gnutella.messages;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.io.IPPortCombo;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.security.QueryKey;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecurityToken;
import org.limewire.util.ByteOrder;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.SimpleFileManager;

/**
 * This class tests the QueryReply class.
 */
@SuppressWarnings("unchecked")
public final class QueryReplyTest extends com.limegroup.gnutella.util.LimeTestCase {

    private static final byte[] IP = new byte[] {1, 1, 1, 1};
    private static final String EXTENSION = "XYZ";
    private static final int MAX_LOCATIONS = 10;

    private QueryReply.GGEPUtil _ggepUtil = new QueryReply.GGEPUtil();
    private FileManager fman = null;
    private Object loaded = new Object();
    
    private SecurityToken _token; 

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
	
    
    public static void globalSetUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        try {
            RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }        
    }
    
	public void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        	    
	    cleanFiles(_sharedDir, false);
	    fman = new SimpleFileManager();
	    PrivilegedAccessor.setValue(RouterService.class, "callback", new FManCallback());
	
        byte[] data = new byte[16];
        new Random().nextBytes(data);
        _token = new QueryKey(data);
	}
		

	/**
	 * Runs the legacy unit test that was formerly in QueryReply.
	 */
	public void testLegacy() throws Exception {		
		byte[] ip={(byte)0xFE, (byte)0, (byte)0, (byte)0x1};
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
		assertEquals("254.0.0.1",qr.getIP());
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
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip
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
		payload[1]=1;                    //non-zero port
		payload[3]=1;                    //non-blank ip
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
		payload[1]=1;                    //non-zero port
		payload[3]=1;                    //non-blank ip		
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
            assertEquals("Sumeet test1", "A",  r.getName());
            assertNull("bad xml", r.getDocument());
        }catch(BadPacketException e){
            fail("metaResponse not created well!", e);
        }

        //Normal case: basic metainfo with no vendor data
        payload=new byte[11+11+(4+1+4+4)+16];
        payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip		
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
		payload[1]=1;            //non-zero port
		payload[3]=1;            //non-blank ip		
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

        assertFalse(qr.getSupportsChat());

        //Weird case.  No common data.  (Don't allow.)
        payload=new byte[11+11+(4+1+2)+16];
        payload[0]=1;            //Number of results
		payload[1]=1;            //non-zero port
        payload[3]=1;           //non-blank ip
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
		payload[1]=1;            //non-zero port
        payload[3]=1;            //non-blank ip
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
		payload[1]=1;                //non-zero port
        payload[3]=1;                //non-blank ip		
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
		payload[1]=1;                //non-zero port
        payload[3]=1;                //non-blank ip				
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
		payload[1]=1;                //non-zero port
        payload[3]=1;                //non-blank ip
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
        assertFalse("LiME!=LIME when looking at private area", qr.getSupportsChat());

        //Create extended QHD from scratch
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          false, true, true, false, true, false);
        assertEquals("254.0.0.1", qr.getIP());
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
        int ggepLen = _ggepUtil.getQRGGEP(true, false, false,
                                          new HashSet(), null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals((23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen, bytes.length);
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)MessageFactory.read(new ByteArrayInputStream(bytes));
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
        ggepLen = _ggepUtil.getQRGGEP(true, false, false,
                                      new HashSet(), null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(
            (23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen,
            bytes.length
        );
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)MessageFactory.read(new ByteArrayInputStream(bytes));
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());

        //Create extended QHD from scratch with different bits set
        // Do not set multicast, as that will unset pushing, busy, etc..
        // and generally confuse the test.
        responses=new Response[2];
        responses[0]=new Response(11,22,"SMDNKD.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"OneMore file  ");
        // first take input of proxies
        String[] hosts = {"www.limewire.com", "www.limewire.org",
                          "www.susheeldaswani.com"};
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        for (int i = 0; i < hosts.length; i++)
            proxies.add(new IpPortImpl(hosts[i], 6346));
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid, new byte[0],
                          true, false, false, true, false, false, true,
                          proxies, null);
        
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertTrue(qr.getSupportsFWTransfer());

        //And check raw bytes....
        out=new ByteArrayOutputStream();
        qr.write(out);

        bytes=out.toByteArray();
        ggepLen = _ggepUtil.getQRGGEP(true, false, true,
                                      proxies, null).length;
        //Length includes header, query hit header and footer, responses, and
        //QHD (public and private)
        assertEquals(
            (23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1+1)+ggepLen,
            bytes.length
        );
        assertEquals(0x3d, bytes[bytes.length-16-6-ggepLen]); //11101
        assertEquals(0x31, bytes[bytes.length-16-5-ggepLen]); //10001

        // check read back....
        qr=(QueryReply)MessageFactory.read(new ByteArrayInputStream(bytes));
        assertEquals("LIME", qr.getVendor());
        assertTrue(qr.getNeedsPush());
        assertFalse(qr.getIsBusy());
        assertFalse(qr.getHadSuccessfulUpload());
        assertTrue(qr.getIsMeasuredSpeed());
        assertFalse(qr.getSupportsChat());
        assertTrue(qr.getSupportsBrowseHost());
        assertFalse(qr.isReplyToMulticastQuery());
        assertTrue(qr.getSupportsFWTransfer());

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
            testGGEP = new GGEP(_ggepUtil.getQRGGEP(false, false, false,
                                                    new HashSet(), null), 
                                0, null);
            assertTrue(false);
        }
        catch (BadGGEPBlockException expected) {}

        // test just BH GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, false, false,
                                                new HashSet(), null), 
                            0, null);
        assertEquals(1, testGGEP.getHeaders().size());
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(!testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

        // test just multicast GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(false, true, false,
                                                new HashSet(), null), 
                            0, null);
        assertEquals(1, testGGEP.getHeaders().size());
        assertTrue(!testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

        // test combo GGEP....
        testGGEP = new GGEP(_ggepUtil.getQRGGEP(true, true, false,
                                                new HashSet(), null),
                            0, null);
        assertEquals(2, testGGEP.getHeaders().size());
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));

    }
    
    public void testGGEPUtilWritesSecurityToken() throws Exception {
        // assert token is written
        GGEP ggep = new GGEP(_ggepUtil.getQRGGEP(false, false, false, null, _token), 0, null);
        assertTrue(ggep.hasKey(GGEP.GGEP_HEADER_SECURE_OOB));
        assertEquals(_token.getBytes(), ggep.get(GGEP.GGEP_HEADER_SECURE_OOB));
        
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, null, _token), 0, null);
        assertTrue(ggep.hasKey(GGEP.GGEP_HEADER_SECURE_OOB));
        assertEquals(_token.getBytes(), ggep.get(GGEP.GGEP_HEADER_SECURE_OOB));
        
        Set<IpPort> proxies = new HashSet<IpPort>();
        proxies.add(new Endpoint("127.0.0.1:6464"));
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, proxies, _token), 0, null);
        assertTrue(ggep.hasKey(GGEP.GGEP_HEADER_SECURE_OOB));
        assertEquals(_token.getBytes(), ggep.get(GGEP.GGEP_HEADER_SECURE_OOB));
        
        // assert token is not written
        try {
            ggep = new GGEP(_ggepUtil.getQRGGEP(false, false, false, null, null), 0, null);
            fail("exception should have been thrown");
        }
        catch (BadGGEPBlockException bgbe) {
        }
                
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, null, null), 0, null);
        assertFalse(ggep.hasKey(GGEP.GGEP_HEADER_SECURE_OOB));
        
        ggep = new GGEP(_ggepUtil.getQRGGEP(true, true, true, proxies, null), 0, null);
        assertFalse(ggep.hasKey(GGEP.GGEP_HEADER_SECURE_OOB));
    }


    public void testBasicPushProxyGGEP() throws Exception {
        basicTest(false, false, false);
        basicTest(false, true, false);
        basicTest(true, false, false);
        basicTest(true, true, false);
        basicTest(false, false, true);
        basicTest(false, true, true);
        basicTest(true, false, true);
        basicTest(true, true, true);
    }

    public void basicTest(final boolean browseHost, 
                          final boolean multicast,
                          final boolean fwTransfer) throws Exception {
        int numHeaders = 1;

        // first take input of proxies
        String[] hosts = {"www.limewire.com", "www.limewire.org",
                          "www.susheeldaswani.com", "www.stanford.edu"};
        Set proxies = new TreeSet(IpPort.COMPARATOR);
        for (int i = 0; i < hosts.length; i++)
            proxies.add(new IpPortImpl(hosts[i], 6346));
        GGEP testGGEP = new GGEP(_ggepUtil.getQRGGEP(browseHost, multicast, 
                                                     fwTransfer, proxies, null), 
                                 0, null);
        if (browseHost) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        }
        if (multicast) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_MULTICAST_RESPONSE));
        }
        if (fwTransfer) {
            numHeaders++;
            assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_FW_TRANS));
        }
        assertTrue(testGGEP.hasKey(GGEP.GGEP_HEADER_PUSH_PROXY));
        assertEquals(numHeaders, testGGEP.getHeaders().size());
        Set retProxies = _ggepUtil.getPushProxies(testGGEP);
        assertEquals(4, retProxies.size());
        assertEquals(retProxies, proxies);
    }


    public void testBadPushProxyInput() throws Exception {
        byte[] badBytes = new byte[6];
        GGEP ggep = null;
        // test a bad ip
        // 0.0.0.0 is a bad address

        // trying to input
        try {
            new IPPortCombo("0.0.0.0", 6346);
            fail("allowed bad PPI");
        } catch (IllegalArgumentException expected) {}

        // from the network
        ggep = new GGEP();
        badBytes[0] = (byte) 0;
        badBytes[1] = (byte) 0;
        badBytes[2] = (byte) 0;
        badBytes[3] = (byte) 0;
        badBytes[4] = (byte) 3;
        badBytes[5] = (byte) 4;
        ggep.put(GGEP.GGEP_HEADER_PUSH_PROXY, badBytes);
        assertEquals(0, _ggepUtil.getPushProxies(ggep).size());

        // test a bad port

        // trying to input is the only case
        try {
            new IPPortCombo("0.0.0.0", 634600);
            fail("allowed bad PPI");
        } catch (IllegalArgumentException expected) {}

        // this should work fine...
        ggep = new GGEP();
        badBytes[0] = (byte) 1;
        badBytes[1] = (byte) 1;
        badBytes[2] = (byte) 2;
        badBytes[3] = (byte) 2;
        badBytes[4] = (byte) 0;
        badBytes[5] = (byte) 0;
        ggep.put(GGEP.GGEP_HEADER_PUSH_PROXY, badBytes);
        assertNotNull(_ggepUtil.getPushProxies(ggep));


        // try to get proxies when the lengths are wrong
        for (int i = 0; i < 100; i++) {
            badBytes = new byte[i];
            // just put some filler in here....
            for (int j = 0; j < badBytes.length; j++) {
                // make sure each one is unique, as the returned
                // Set from getPushProxies will filter duplicates
                if(j%6 == 0) {
                    badBytes[j] = (byte)j;
                }
                else{
                    badBytes[j] = (byte)i;
                }
            }
            ggep = new GGEP();
            ggep.put(GGEP.GGEP_HEADER_PUSH_PROXY, badBytes);
            if (i == 0)
                assertEquals(0, _ggepUtil.getPushProxies(ggep).size());
            else if (i < 6) 
                assertEquals(0, _ggepUtil.getPushProxies(ggep).size());
            else  {// length is fine
                   // -1 because the first entry is invalid since it
                   // begins with 0.               
                assertEquals((i / 6) - 1,
                             _ggepUtil.getPushProxies(ggep).size());
                
            }
        }

    }


    public void testManualPushProxyInput() throws Exception {
        Random rand = new Random();
        GGEP ggep = null;
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[6*(i+1)];
            rand.nextBytes(bytes);
            // don't trust zeroes or 255 in IP...
            for (int j = 0; j < bytes.length; j++) {
                if (bytes[j] == (byte) 0) bytes[j] = (byte) 1;
                if (bytes[j] == (byte)255)bytes[j] = (byte)254;
            }

            // from the network
            ggep = new GGEP();
            ggep.put(GGEP.GGEP_HEADER_PUSH_PROXY, bytes);

            Set proxies = _ggepUtil.getPushProxies(ggep);
            assertNotNull(proxies);

            Iterator iter = proxies.iterator();
            int j = 0;
            while(iter.hasNext()) {
                final int inIndex = 6*j;
                iter.next(); //IpPort ppi = (IpPort)iter.next();
               // InetAddress addr = ppi.getInetAddress();
                byte[] tempAddr = new byte[4];
                System.arraycopy(bytes, inIndex, tempAddr, 0, 4);

                InetAddress addr2 = InetAddress.getByAddress(tempAddr);
                String address = addr2.getHostAddress();
                
                int curPort = ByteOrder.leb2int(bytes, inIndex+4, 2);
                IpPort ppi2 = 
                    new IpPortImpl(address, curPort);

                assertTrue(proxies.contains(ppi2));
                //assertEquals(addr, addr2);
                //int port = ppi.getPushProxyPort();
                /*
                final int inIndex = 6*j;
                
                byte[] addrBytes = addr.getAddress();
                byte[] portBytes = new byte[2];
                ByteOrder.short2leb((short)port, portBytes, 0);
                
                assertEquals(addrBytes[0], bytes[inIndex]);
                assertEquals(addrBytes[1], bytes[inIndex+1]);
                assertEquals(addrBytes[2], bytes[inIndex+2]);
                assertEquals(addrBytes[3], bytes[inIndex+3]);
                assertEquals(portBytes[0], bytes[inIndex+4]);
                assertEquals(portBytes[1], bytes[inIndex+5]);
                */
                j++;
            }
        }

    }
    
    public void testPushProxyQueryReply() throws Exception {
        String[] hosts = {"www.limewire.com", "www.limewire.org",
                          "www.susheeldaswani.com", "www.berkeley.edu"};

        for (int outer = 0; outer < hosts.length; outer++) {
            //PushProxyInterface[] proxies = new PushProxyInterface[outer+1];
            Set proxies = new TreeSet(IpPort.COMPARATOR);
            for (int i = 0; i < hosts.length; i++)
                proxies.add(
                    new IpPortImpl(hosts[i], 6346));
            
            QueryReply qr = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           proxies);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qr.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            QueryReply readQR = (QueryReply) MessageFactory.read(bais);

            // test read from network            
            Set retProxies = readQR.getPushProxies();
            assertNotNull(retProxies);
            assertTrue(retProxies != proxies);
            assertEquals(retProxies.size(), proxies.size());
            assertEquals(retProxies, proxies);
            assertEquals(proxies, retProxies);

            // test simple accessor
            retProxies = qr.getPushProxies();
            assertNotNull(retProxies);
            assertTrue(retProxies != proxies);
            assertEquals(retProxies.size(), proxies.size());
            assertEquals(retProxies, proxies);
            assertEquals(proxies, retProxies);
        }
    }
    
    public void testQueryReplyHasAlternates() throws Exception {
        addFilesToLibrary();
        addAlternateLocationsToFiles();
        
        boolean checked = false;
		for(int i = 0; i < fman.getNumFiles(); i++) {
			FileDesc fd = fman.get(i);
			Response testResponse = new Response(fd);

            String name = fd.getFileName();
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = QueryRequest.createQuery(fd.getFileName());
			Response[] hits = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
            if(hits.length == 0)
                fail("no hit for query: " + qr);
			if ( hits.length != 1 )
                continue;
            
            checked = true;
			
			// first check basic stuff on the response.
			assertEquals("responses should be equal", testResponse, hits[0]);
			assertEquals("should have 10 other alts",
			    10, testResponse.getLocations().size());
			assertEquals("should have equal alts",
			    testResponse.getLocations(), hits[0].getLocations());
			    
			// then actually create a QueryReply and read it, to make
			// sure we can write & read stuff correctly.
            QueryReply qReply = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, hits,
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);			    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qReply.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            QueryReply readQR = (QueryReply) MessageFactory.read(bais);
            
            List readHits = readQR.getResultsAsList();
            assertEquals("wrong # of results", hits.length, readHits.size());
            Response hit = (Response)readHits.get(0);
            assertEquals("wrong # of alts",
                hits[0].getLocations(), hit.getLocations());
		}        
        
        assertTrue("didn't check anything!", checked);
    }
    
    public void testQueryReplyHasCreationTimes() throws Exception {
        addFilesToLibrary();
        addCreationTimeToFiles();
        boolean checked = false;
		for(int i = 0; i < fman.getNumFiles(); i++) {
			FileDesc fd = fman.get(i);
			long expectTime = (fd.getIndex() + 1) * 10013;
			Response testResponse = new Response(fd);
			assertEquals(fd.toString(), expectTime, testResponse.getCreateTime());
            
            String name = fd.getFileName();
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = QueryRequest.createQuery(fd.getFileName());
			Response[] hits = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
            if(hits.length == 0)
                fail("no reply for query: " + qr);
			if ( hits.length != 1 )
                continue;
			checked = true;
			
			// first check basic stuff on the response.
			assertEquals("responses should be equal", testResponse, hits[0]);
            assertEquals("wrong creation time", expectTime,
                                                hits[0].getCreateTime());
			    
			// then actually create a QueryReply and read it, to make
			// sure we can write & read stuff correctly.
            QueryReply qReply = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, hits,
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);			    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qReply.write(baos);
            ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
            QueryReply readQR = (QueryReply) MessageFactory.read(bais);
            
            List readHits = readQR.getResultsAsList();
            assertEquals("wrong # of results", hits.length, readHits.size());
            Response hit = (Response)readHits.get(0);
            // rounds off to seconds (drops milliseconds)
            assertEquals("wrong creation time", expectTime / 1000 * 1000,
                                                hit.getCreateTime());
		}
        
        assertTrue("didn't check anything!", checked);
    }

    /**
     * Test to make sure that results that have no name are rejected 
     */
    public void testThatEmptyResultsAreRejected() throws Exception {

 		// create a payload that says it has one result, but whose
        // result is empty.  This should be rejected!
		byte[] payload=new byte[11+11+16];
		payload[0] = 1;            //Number of results
		payload[1]=1;              //non-zero port
        payload[3]=1;                //non-blank ip
		//payload[11+8]=(byte)65;  //The character 'A'

		QueryReply qr = 
            new QueryReply(new byte[16], (byte)5, (byte)0, payload);

        
        try {
            qr.getResultsAsList();
            fail("should have thrown an exception for empty result");
        } catch(BadPacketException e) {
        }
    }
    
    public void testSecureReplyNoSignature() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        GGEP ggep = new GGEP(false);
        ggep.put("SB");
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertTrue(reply.hasSecureData());
        assertNull(reply.getSecureSignature());
        runSignatureTest(reply, indexes, payload.toByteArray());
    }
    
    public void testSecureReplyWithSignature() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        GGEP ggep = new GGEP(false);
        ggep.put("SB");
        byte[] sig = new byte[100];
        new Random().nextBytes(sig);
        ggep.put("SIG", sig);
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertTrue(reply.hasSecureData());
        assertEquals(sig, reply.getSecureSignature());
        runSignatureTest(reply, indexes, payload.toByteArray());
    }
    
    public void testNoSecureReply() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        GGEP ggep = new GGEP(false);
        ggep.put("SC");
        byte[] sig = new byte[100];
        new Random().nextBytes(sig);
        ggep.put("SIG", sig);        
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertFalse(reply.hasSecureData());
        assertNull(reply.getSecureSignature());
        Provider provider = new TestProvider();
        Signature signature = Signature.getInstance("FakeSignature", provider);
        signature.initSign(null);
        reply.updateSignatureWithSecuredBytes(signature);
        assertEquals(-1, FakeSignatureSpi.off1);
        assertEquals(-1, FakeSignatureSpi.off2);
        assertEquals(-1, FakeSignatureSpi.len1);
        assertEquals(-1, FakeSignatureSpi.len2);
        assertNull(FakeSignatureSpi.update1);
        assertNull(FakeSignatureSpi.update2);       
    }
    
    public void testSecureStatus() throws Exception {
        int indexes[] = new int[2];
        ByteArrayOutputStream payload = new ByteArrayOutputStream();        
        GGEP ggep = new GGEP(false);
        QueryReply reply = newSecureQueryReply(ggep, indexes, payload);
        assertEquals(SecureMessage.INSECURE, reply.getSecureStatus());
        reply.setSecureStatus(SecureMessage.FAILED);
        assertEquals(SecureMessage.FAILED, reply.getSecureStatus());
        reply.setSecureStatus(SecureMessage.SECURE);
        assertEquals(SecureMessage.SECURE, reply.getSecureStatus());
    }
    
    public void testSecurityTokenBytesSetAndParsed() throws IllegalArgumentException, IOException, BadPacketException {
        Response r = new Response(0, 1, "test");
        QueryReply query = new QueryReply(GUID.makeGuid(), (byte)1, 1459, 
                InetAddress.getLocalHost().getAddress(), 30945L, new Response[] { r },
                GUID.makeGuid(), new byte[0], false, false, false, false, false,
                false, false, IpPort.EMPTY_SET, _token);
                
        assertEquals(_token.getBytes(), query.getSecurityToken());
        
        // test copy constructor preserves security bytes
        query = new QueryReply(GUID.makeGuid(), query);
        assertEquals(_token.getBytes(), query.getSecurityToken());
        
        // test network constructor
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        query.writePayload(out);
        query = new QueryReply(GUID.makeGuid(), (byte)1, (byte)1, out.toByteArray());
        assertEquals(_token.getBytes(), query.getSecurityToken());
    }
    
    public void testIsLikelyNotFirewalled() throws Exception {
        
        LocalSocketAddressProvider oldProvider = LocalSocketAddressService.getSharedProvider();
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProvider() {

            public byte[] getLocalAddress() {
                return null;
            }

            public int getLocalPort() {
                // TODO Auto-generated method stub
                return 0;
            }

            public boolean isLocalAddressPrivate() {
                return true;
            }
            
        });
        
        try {
            assertFalse(createReply(new byte[] { 127, 0, 0, 1 }, null).isLikelyNotFirewalled());
            
            Set<IpPort> proxies = new HashSet<IpPort>();
            proxies.add(new IpPortImpl("limewire.com:5454"));
            assertFalse(createReply(new byte[] { 127, 0, 0, 1 }, proxies).isLikelyNotFirewalled());
            
            byte[] addr = InetAddress.getByName("limewire.com").getAddress();
            assertFalse(createReply(addr, proxies).isLikelyNotFirewalled());
            assertTrue(createReply(addr, null).isLikelyNotFirewalled());
            assertTrue(createReply(addr, IpPort.EMPTY_SET).isLikelyNotFirewalled());
        }
        finally {
            LocalSocketAddressService.setSocketAddressProvider(oldProvider);
        }
    }
    
    private static QueryReply createReply(byte[] address, Set<IpPort> proxies) {
        Response[] res = new Response[] { new Response(2, 5, "response") }; 
        
        return new QueryReply(GUID.makeGuid(), (byte)1, (byte)1, address, 
                0, res, GUID.makeGuid(),
                new byte[0], false, false, true, true, false, false, true,
                proxies, null);
    }
    
    private void runSignatureTest(QueryReply reply, int[] indexes, byte[] payload) throws Exception {
        Provider provider = new TestProvider();
        Signature sig = Signature.getInstance("FakeSignature", provider);
        sig.initSign(null);
        reply.updateSignatureWithSecuredBytes(sig);
        assertNotEquals(-1, FakeSignatureSpi.off1);
        assertNotEquals(-1, FakeSignatureSpi.off2);
        assertNotEquals(-1, FakeSignatureSpi.len1);
        assertNotEquals(-1, FakeSignatureSpi.len2);
        assertNotNull(FakeSignatureSpi.update1);
        assertNotNull(FakeSignatureSpi.update2);
        
        assertEquals(0, FakeSignatureSpi.off1);
        assertEquals(indexes[0], FakeSignatureSpi.len1);
        for(int i = 0; i < indexes[0]; i++)
            assertEquals("bad match at offset: " + i, payload[i], FakeSignatureSpi.update1[i]);
        
        assertEquals(indexes[1], FakeSignatureSpi.off2);
        int len2 = payload.length - 16 - indexes[1];
        assertEquals(len2, FakeSignatureSpi.len2);
        for(int i = indexes[1]; i < len2 + indexes[1]; i++)
            assertEquals("bad match at offset: " + i, payload[i], FakeSignatureSpi.update2[i]);
    }
    
    private QueryReply newSecureQueryReply(GGEP secureGGEP, int[] indexes, ByteArrayOutputStream payload) throws Exception {
        indexes[0] = -1;
        indexes[1] = -1;
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(1); // # of results
        ByteOrder.short2leb((short)6346, out); // port
        out.write(IP); // ip
        ByteOrder.int2leb(1, out);
        Response r = new Response(0, 1, "test");
        r.writeToStream(out);
        out.write(new byte[] { 'L', 'I', 'M', 'E' });
        out.write(4); // common payload length
        out.write(0x3C); // flags (control no push) 
        out.write(0x21); // control (yes ggep, flag busy)
        ByteOrder.short2leb((short)1, out); // xml size
        out.write(0); // no chat
        
        GGEP ggep = new GGEP(false);
        ggep.put("test", "data");
        ggep.write(out); // normal ggep block.
        
        indexes[0] = out.size();
        secureGGEP.write(out);
        indexes[1] = out.size();
        
        out.write(0); // null after XML
        out.write(new byte[16]); // clientGUID
        
        // copy the payload.
        payload.reset();
        out.writeTo(payload);
        
        return new QueryReply(new byte[16], (byte)1, (byte)1, out.toByteArray());
    }

    private void addFilesToLibrary() throws Exception {
        String dirString = "com/limegroup/gnutella";
        File testDir = CommonUtils.getResourceFile(dirString);
        testDir = testDir.getCanonicalFile();
        assertTrue("could not find the gnutella directory", testDir.isDirectory());

        File[] testFiles = testDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                // use files with a $ because they'll generally
                // trigger a single-response return, which is
                // easier to check
                return FileManager.isFilePhysicallyShareable(file) && file.getName().indexOf("$") != -1;
            }
        });

        assertNotNull("no files to test against", testFiles);
        assertNotEquals("no files to test against", 0, testFiles.length);

        for (int i = 0; i < testFiles.length; i++) {
            File shared = new File(_sharedDir, testFiles[i].getName() + "." + EXTENSION);
            assertTrue("unable to get file", FileUtils.copy(testFiles[i], shared));
        }

        waitForLoad();
        assertEquals("unexpected number of shared files", testFiles.length, fman.getNumFiles());
    }
    
    private void addAlternateLocationsToFiles() throws Exception {
        FileDesc[] fds = fman.getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++) {
            String urn = fds[i].getSHA1Urn().httpStringValue();
            for(int j = 0; j < MAX_LOCATIONS + 5; j++) {
                String loc = "http://1.2.3." + j + ":6346/uri-res/N2R?" + urn;
                RouterService.getAltlocManager().add(AlternateLocation.create(loc), null);
            }
        }
    }
    
    private void addCreationTimeToFiles() throws Exception {
        FileDesc[] fds = fman.getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++) {
            long time = (fds[i].getIndex() + 1) * 10013;
            CreationTimeCache.instance().addTime(fds[i].getSHA1Urn(), time);
            CreationTimeCache.instance().commitTime(fds[i].getSHA1Urn());
        }
    }        
    
    private class FManCallback extends ActivityCallbackStub {
        public void fileManagerLoaded() {
            synchronized(loaded) {
                loaded.notify();
            }
        }
    }
    
    private void waitForLoad() {
        synchronized(loaded) {
            try {
                fman.loadSettings();
                loaded.wait();
            } catch (InterruptedException e) {
                //good.
            }
        }
    }
    
    
    /*
     * All the below crap is because we can't subclass Signature and instead need to provide an SPI.
     */
    
    /** Provider that references the fake SPI */
    private static final class TestProvider extends Provider {
        TestProvider() {
            super("LIME", 1.0, "LIME test provider");
            put("Signature.FakeSignature", FakeSignatureSpi.class.getName());
        }
    }
    
    /** SPI that stores things statically because there's no other reference to it anywhere. */
    @SuppressWarnings("deprecation")
    public static class FakeSignatureSpi extends SignatureSpi {
        private static byte[] update1;
        private static int off1;
        private static int len1;
        
        private static byte[] update2;
        private static int off2;
        private static int len2;

        @SuppressWarnings("deprecation")
        protected Object engineGetParameter(String arg0) throws InvalidParameterException {
            return null;
        }

        protected void engineInitSign(PrivateKey arg0) throws InvalidKeyException {
            update1 = null;
            off1 = -1;
            len1 = -1;
            update2 = null;
            off2 = -1;
            len2 = -1;
        }

        protected void engineInitVerify(PublicKey arg0) throws InvalidKeyException {
        }

        @SuppressWarnings("deprecation")
        protected void engineSetParameter(String arg0, Object arg1) throws InvalidParameterException {
        }

        protected byte[] engineSign() throws SignatureException {
            return null;
        }

        protected void engineUpdate(byte arg0) throws SignatureException {
        }

        protected void engineUpdate(byte[] data, int off, int len) throws SignatureException {
            if(update1 == null) {
                update1 = data;
                off1 = off;
                len1 = len;
            } else if(update2 == null) {
                update2 = data;
                off2 = off;
                len2 = len;
            } else {
                fail("updating signature more than twice!");
            }
        }

        protected boolean engineVerify(byte[] arg0) throws SignatureException {
            return false;
        }
    }
}
