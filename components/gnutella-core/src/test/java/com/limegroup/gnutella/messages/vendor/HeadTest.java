package com.limegroup.gnutella.messages.vendor;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.Interval;
import org.limewire.collection.IntervalSet;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.ManagedConnectionStub;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.IncompleteFileDescStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.UploadManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

import de.vdheide.mp3.ByteOrder;

/**
 * this class tests the handling of udp head requests and responses.
 */
@SuppressWarnings({"unchecked", "unused"})
public class HeadTest extends LimeTestCase {

	/**
	 * keep a file manager which shares one complete file and one incomplete file
	 */
	private static FileManagerStub _fm;
    private static UploadManagerStub _um;
	
	/**
	 * URNs for the 3 files that will be requested
	 */
    private static URN _haveFull,_notHave,_havePartial, _tlsURN;
	
	/**
	 * file descs for the partial and complete files
	 */
    private static IncompleteFileDescStub _partial;
    private static FileDescStub _complete;
	/**
	 * an interval that can fit in a packet, and one that can't
	 */
    private static IntervalSet _ranges, _rangesMedium, _rangesJustFit, _rangesTooBig;
	
    private static PushEndpoint pushCollectionPE, tlsCollectionPE;
    
    
	
    private static int PACKET_SIZE;
    
    private static RemoteFileDesc blankRFD;
	
	
	public HeadTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(HeadTest.class);
	}
	
	/**
	 * sets up the testing environment for the UDPHeadPong.
	 * two files are shared - _complete and _incomplete
	 * _complete has altlocs _altlocCollectionComplete
	 * _incomplete has altlocs _altlocCollectionIncomplete
	 * _incomplete has available ranges _ranges
	 * 
	 * @throws Exception
	 */
	public static void globalSetUp() throws Exception{
	    
	    SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
	    
	    MessageRouterStub mrStub = new MessageRouterStub();
	    
	    PrivilegedAccessor.setValue(RouterService.class,"messageRouter",mrStub);
	    
	    ManagedConnectionStub mStub = new ManagedConnectionStub();
	    final Set conns = new HashSet();
	    conns.add(mStub);
	    
	    ConnectionManagerStub cmStub = new ConnectionManagerStub() {
	        public Set getPushProxies() {
	            return conns;
	        }
	    };
	    
	    PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
		//PrivilegedAccessor.setValue(RouterService.class,"acceptor", new AcceptorStub());
		_fm = new FileManagerStub();
		_um = new UploadManagerStub();
		
		int base=0;
		_ranges = new IntervalSet();
		for (int i=2;i<10;i++) {
			int low = base;
			_ranges.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesMedium = new IntervalSet();
		for (int i=2;i<70;i++) {
			int low = base;
			_rangesMedium.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesJustFit = new IntervalSet();
		for (int i=2;i<73;i++) {
			int low = base;
			_rangesJustFit.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesTooBig = new IntervalSet();
		for (int i=2;i<220;i++) {
			int low = base;
			_rangesTooBig.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		_haveFull =    URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
		_notHave =      FileManagerStub._notHave;
		_havePartial = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFD");
        _tlsURN =      URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYTLS");
		
		_partial = new IncompleteFileDescStub("incomplete",_havePartial,3);
		_partial.setRangesByte(_ranges.toBytes());
		_complete = new FileDescStub("complete",_haveFull,2);		
		
		Map urns = new HashMap();
		urns.put(_havePartial,_partial);
		urns.put(_haveFull,_complete);
		List descs = new LinkedList();
		descs.add(_partial);
		descs.add(_complete);
		
		_fm = new FileManagerStub(urns,descs);
		
		assertEquals(_partial,_fm.getFileDescForUrn(_havePartial));
		assertEquals(_complete,_fm.getFileDescForUrn(_haveFull));
		
		PrivilegedAccessor.setValue(HeadPong.class, "_fileManager",_fm);
		PrivilegedAccessor.setValue(HeadPong.class, "_uploadManager",_um);
		
		PACKET_SIZE = ((Integer)PrivilegedAccessor.getValue(HeadPong.class,"PACKET_SIZE")).intValue();
    }
    
    public void setUp() throws Exception {
        blankRFD = new RemoteFileDesc("1.1.1.1", 1, 1, "file", 1, new byte[16], 1, false, -1, false, null, null, false, false, null, null, -1, false);
        assertFalse(blankRFD.isBrowseHostEnabled());
        assertFalse(blankRFD.isChatEnabled());
        assertFalse(blankRFD.isBusy());
        assertFalse(blankRFD.isDownloading());
        assertFalse(blankRFD.isFirewalled());
        assertFalse(blankRFD.isPartialSource());
        assertFalse(blankRFD.isReplyToMulticast());
        assertFalse(blankRFD.isTLSCapable());
        assertNull(blankRFD.getPushAddr());
        assertEquals(0, blankRFD.getPushProxies().size());
        assertEquals(Integer.MAX_VALUE, blankRFD.getQueueStatus());
        
        // Get a fresh environment for each test
        ((Map)PrivilegedAccessor.getValue(PushEndpoint.class, "GUID_PROXY_MAP")).clear();
        RouterService.getAltlocManager().purge();
        createCollections();
    }
    
    private void clearStoredProxies() throws Exception {
        ((Map)PrivilegedAccessor.getValue(PushEndpoint.class, "GUID_PROXY_MAP")).clear();
    }
    
    /** Test TLS response. */
    public void testTLSPong() throws Exception {
        HeadPing ping = new HeadPing(_notHave); // this is just so we can generate a HeadPong
        HeadPong response1, response2;
        
        ConnectionSettings.TLS_INCOMING.setValue(true);
        response1 = new HeadPong(ping);
        
        ConnectionSettings.TLS_INCOMING.setValue(false);
        response2 = new HeadPong(ping);

        // Must clear the proxies before reparsing, else static things are re-used.
        clearStoredProxies();
        
        HeadPong pong = reparse(response1);
        assertTrue(pong.isTLSCapable());
        pong.updateRFD(blankRFD);
        assertTrue(blankRFD.isTLSCapable());
        
        ConnectionSettings.TLS_INCOMING.setValue(false);
        pong = reparse(response2);
        assertFalse(pong.isTLSCapable());
        pong.updateRFD(blankRFD);
        assertFalse(blankRFD.isTLSCapable());
    }
	
	/**
	 * tests the scenario where the file cannot be found.
	 */
	public void testFileNotFound() throws Exception {
		HeadPing ping = new HeadPing(_notHave);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
		pong = reparse(pong);
		
		assertEquals(pong.getGUID(),ping.getGUID());
		
		assertFalse(pong.hasFile());
		assertFalse(pong.hasCompleteFile());
		assertNull(pong.getAltLocs());
		assertNull(pong.getRanges());
	}
	
	/**
	 * tests the scenarios where an incomplete and complete files are
	 * behind firewall or open.
	 */
	public void testFirewalledNoAcceptedIncoming() throws Exception {
		PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming", Boolean.FALSE);
		assertFalse(RouterService.acceptedIncomingConnection());
		
		HeadPing ping = new HeadPing(_haveFull);
		HeadPing pingi = new HeadPing(_havePartial);
		HeadPong pong = new HeadPong(ping);
		HeadPong pongi = new HeadPong(pingi);
        clearStoredProxies();
        pong = reparse(pong);
        pongi = reparse(pongi);
		
		assertEquals(ping.getGUID(),pong.getGUID());
		assertEquals(pingi.getGUID(),pongi.getGUID());
		
		assertTrue(pong.hasFile());
		assertTrue(pong.isFirewalled());
		assertTrue(pong.hasCompleteFile());
		assertNull(pong.getAltLocs());
		assertNull(pong.getRanges());
		
		assertTrue(pongi.hasFile());
		assertTrue(pongi.isFirewalled());
		assertFalse(pongi.hasCompleteFile());
		assertNull(pongi.getAltLocs());
		assertNull(pongi.getRanges());
    }
    
    public void testFirewalledAcceptedIncoming() throws Exception {
		PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming", new Boolean(true));
		assertTrue(RouterService.acceptedIncomingConnection());
		
		HeadPing ping = new HeadPing(_haveFull);
        HeadPing pingi = new HeadPing(_havePartial);
		HeadPong pong = new HeadPong(ping);
        HeadPong pongi = new HeadPong(pingi);
        clearStoredProxies();
        pong = reparse(pong);
        pongi = reparse(pongi);
		
		assertEquals(pong.getGUID(),ping.getGUID());
		assertEquals(pingi.getGUID(),pongi.getGUID());
		
		assertFalse(pong.isFirewalled());
		assertTrue(pong.hasFile());
		assertTrue(pong.hasCompleteFile());
		assertNull(pong.getAltLocs());
		assertNull(pong.getRanges());
		
		assertTrue(pongi.hasFile());
		assertFalse(pongi.isFirewalled());
		assertFalse(pongi.hasCompleteFile());
		assertNull(pongi.getAltLocs());
		assertNull(pongi.getRanges());
	}
	
	/**
	 * tests whether the downloading flag is set properly.
	 */
	public void testActivelyDownloading() throws Exception {	
		_partial.setActivelyDownloading(true);
		HeadPing ping = new HeadPing(_havePartial);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
		pong = reparse(pong);
		assertTrue(pong.isDownloading());
    }
    
    public void testNotActivelyDownloading() throws Exception {		
		_partial.setActivelyDownloading(false);
        HeadPing ping = new HeadPing(_havePartial);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);
		assertFalse(pong.isDownloading());
	}
	/**
	 * tests requesting ranges from complete, incomplete files
	 * as well as requesting too big ranges to fit in packet.
	 */
	public void testRangesFit() throws Exception {		
		HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),_haveFull,HeadPing.INTERVALS);
		HeadPing pingi = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.INTERVALS);		
		HeadPong pong = new HeadPong(ping);
		HeadPong pongi = new HeadPong(pingi);
        clearStoredProxies();
        pong = reparse(pong);
        pongi = reparse(pongi);
		
		assertTrue(pong.hasCompleteFile());
		assertFalse(pongi.hasCompleteFile());
		
		assertNull(pong.getRanges());
		assertNotNull(pongi.getRanges());
        
        pongi.updateRFD(blankRFD);
        assertTrue(blankRFD.isPartialSource());
        assertEquals(pongi.getRanges(), blankRFD.getAvailableRanges());
        
        pong.updateRFD(blankRFD);
        assertFalse(blankRFD.isPartialSource());        
		
		assertTrue(Arrays.equals(_ranges.toBytes(),pongi.getRanges().toBytes()));
    }
    
    public void testRangesDontFit() throws Exception {		
		//now make the incomplete file desc carry ranges which are too big to
		//fit in a packet
		_partial.setRangesByte(_rangesTooBig.toBytes());
        try {    		
    		HeadPing pingi = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.INTERVALS);
    		HeadPong pongi = new HeadPong(pingi);
            clearStoredProxies();
            pongi = reparse(pongi);
    		
    		assertNull(pongi.getRanges());
    		assertLessThan(PACKET_SIZE,pongi.getPayload().length);
        } finally {
            _partial.setRangesByte(_ranges.toBytes());
        }
	}
	
	/**
	 * tests various values for the queue rank
	 */
	public void testQueueStatusEmpty() throws Exception {
		HeadPing ping = new HeadPing(_havePartial);		
		HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);
		int allFree =  pong.getQueueStatus();
		assertEquals(-UploadSettings.HARD_MAX_UPLOADS.getValue(), allFree);
        pong.updateRFD(blankRFD);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
    
    public void testQueueStatusSomeTaken() throws Exception {
        HeadPing ping = new HeadPing(_havePartial);     
        _um.setUploadsInProgress(10);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);
		assertEquals(-UploadSettings.HARD_MAX_UPLOADS.getValue()+10,pong.getQueueStatus());
        pong.updateRFD(blankRFD);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
		
    public void testQueueStatusSomeQueued() throws Exception {
        HeadPing ping = new HeadPing(_havePartial);
        _um.setNumQueuedUploads(5);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);
		assertEquals(5,pong.getQueueStatus());
        pong.updateRFD(blankRFD);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
    
    public void testQueueStatusAllTakenNoneQueued() throws Exception {
        HeadPing ping = new HeadPing(_havePartial);
        _um.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue());
        _um.setNumQueuedUploads(0);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);    
		assertEquals(0,pong.getQueueStatus());
        pong.updateRFD(blankRFD);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertFalse(blankRFD.isBusy());
    }
    
    public void testQueueStatusBusy() throws Exception {
        HeadPing ping = new HeadPing(_havePartial);
        _um.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue());
        _um.setNumQueuedUploads(0);
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);  
		
		_um.setIsBusy(true);
		_um.setNumQueuedUploads(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
		pong = reparse(new HeadPong(ping));
		assertGreaterThanOrEquals(0x7F,pong.getQueueStatus());
        pong.updateRFD(blankRFD);
        assertEquals(pong.getQueueStatus(), blankRFD.getQueueStatus());
        assertTrue(blankRFD.isBusy());
	}
	
	/**
	 * tests handling of alternate locations.
	 */
	public void testAltLocsFitWithRanges() throws Exception {		
		//add some big interval that fill most of the packet but not all
		_partial.setRangesByte(_rangesMedium.toBytes());
		
		//ping 1 should contain alternate locations. 
		
		//the second ping should be too big to contain all altlocs.
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_haveFull,HeadPing.ALT_LOCS);
		HeadPing ping2 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,
				HeadPing.ALT_LOCS | HeadPing.INTERVALS);
		
		HeadPong pong1 = new HeadPong(ping1);
		HeadPong pong2 = new HeadPong(ping2);
        clearStoredProxies();
        pong1 = reparse(pong1);
        pong2 = reparse(pong2);
		
		assertNull(pong1.getRanges());
		assertNotNull(pong2.getRanges());
		assertTrue(Arrays.equals(_rangesMedium.toBytes(),pong2.getRanges().toBytes()));
		assertGreaterThan(pong1.getPayload().length,pong2.getPayload().length);
		
		assertLessThan(pong1.getAltLocs().size(),pong2.getAltLocs().size());
		assertLessThan(PACKET_SIZE,pong2.getPayload().length);
    }
    
    public void testAltLocsDontFitBecauseOfTooManyRanges() throws Exception {
		//now test if no locs will fit because of too many ranges
		_partial.setRangesByte(_rangesJustFit.toBytes());
		HeadPing ping2 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,
				HeadPing.ALT_LOCS | HeadPing.INTERVALS);
		HeadPong pong2 = new HeadPong(ping2);
        clearStoredProxies();
        pong2 = reparse(pong2);
        
		
		assertNotNull(pong2.getRanges());
		assertNull(pong2.getAltLocs());
		
		//restore medium ranges to partial file
		_partial.setRangesByte(_rangesMedium.toBytes());
	}
	
	public void testPushAltLocsRequestedButNoLocs() throws Exception {		
		//try with a file that doesn't have push locs
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_haveFull,HeadPing.PUSH_ALTLOCS);
		assertTrue(ping1.requestsPushLocs());
		HeadPong pong1 = new HeadPong(ping1);
        clearStoredProxies();
        pong1 = reparse(pong1);
		assertNull(pong1.getPushLocs());
    }
    
    public void testPushAltLocsReturned() throws Exception {
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.PUSH_ALTLOCS);
		assertTrue(ping1.requestsPushLocs());
		HeadPong pong1 = new HeadPong(ping1);
        clearStoredProxies();
        pong1 = reparse(pong1);

		assertNull(pong1.getRanges());
		assertNull(pong1.getAltLocs());
		assertNotNull(pong1.getPushLocs());
		
		RemoteFileDesc dummy = 
			new RemoteFileDesc("www.limewire.org", 6346, 10, "asdf", 
			        		10, GUID.makeGuid(), 10, true, 2, true, null, 
							   HugeTestUtils.URN_SETS[1],
                               false,false,"",null, -1, false);
		
		Set received = pong1.getAllLocsRFD(dummy);
		assertEquals(1,received.size());
		RemoteFileDesc rfd = (RemoteFileDesc)received.toArray()[0]; 
		PushEndpoint point = rfd.getPushAddr();
		assertEquals(pushCollectionPE,point);
		assertEquals(pushCollectionPE.getProxies().size(),point.getProxies().size());
        Set parsedProxies = new IpPortSet(point.getProxies());
		parsedProxies.retainAll(pushCollectionPE.getProxies());
		assertEquals(pushCollectionPE.getProxies().size(),parsedProxies.size());
    }
    
    public void testPushAltLocsWantOnlyFWT() throws Exception {
		//now ask only for fwt push locs - nothing returned
		HeadPing ping1 = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,HeadPing.PUSH_ALTLOCS | HeadPing.FWT_PUSH_ALTLOCS);
		assertTrue(ping1.requestsFWTPushLocs());
		HeadPong pong1 = new HeadPong(ping1);
        clearStoredProxies();
        pong1 = reparse(pong1);
		assertNull(pong1.getPushLocs());
	}
    
    public void testTLSPushLocs() throws Exception {
        HeadPing ping = new HeadPing(new GUID(), _tlsURN, HeadPing.PUSH_ALTLOCS);
        assertTrue(ping.requestsPushLocs());
        HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);
        assertNull(pong.getRanges());
        assertNull(pong.getAltLocs());
        assertNotNull(pong.getPushLocs());
        assertEquals(1, pong.getPushLocs().size());
        Set<? extends IpPort> proxies = pong.getPushLocs().iterator().next().getProxies();
        assertEquals(3, proxies.size());
        // the proxies: 2.3.4.5:5;3.4.5.6:7;4.5.6.7:8
        Set expectedProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"), new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        expectedProxies.retainAll(proxies);
        assertEquals(3, expectedProxies.size());
        // We expect the 3.4.5.6 & 4.5.6.7 to be the TLS ones.
        int tls = 0;
        IpPort nonTLS = null;        
        for(IpPort ipp : proxies) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls++;
            else
                nonTLS = ipp;
        }
        assertEquals(2, tls);
        assertEquals("2.3.4.5", nonTLS.getAddress());
        
        RemoteFileDesc dummy = 
            new RemoteFileDesc("www.limewire.org", 6346, 10, "asdf", 
                            10, GUID.makeGuid(), 10, true, 2, true, null, 
                               HugeTestUtils.URN_SETS[1],
                               false,false,"",null, -1, false);
        
        Set rfds = pong.getAllLocsRFD(dummy);
        assertEquals(1, rfds.size());
        RemoteFileDesc rfd = (RemoteFileDesc)rfds.toArray()[0]; 
        assertEquals(tlsCollectionPE.getClientGUID(), rfd.getPushAddr().getClientGUID());
        assertEquals(3, rfd.getPushProxies().size());
        Set parsedProxies = new IpPortSet(rfd.getPushProxies());
        parsedProxies.retainAll(expectedProxies);
        assertEquals(3, parsedProxies.size());
        tls = 0;
        nonTLS = null;        
        for(IpPort ipp : rfd.getPushProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls++;
            else
                nonTLS = ipp;
        }
        assertEquals(2, tls);
        assertEquals("2.3.4.5", nonTLS.getAddress());        
    }
    
    public void testTLSPushLocsWithOldHeadPing() throws Exception {
        HeadPing ping = new HeadPing(new GUID(), _tlsURN, HeadPing.PUSH_ALTLOCS);
        assertTrue(ping.requestsPushLocs());
        HeadPong pong = new HeadPong(reversion(ping, (short)1));
        clearStoredProxies();
        pong = reparse(pong);
        assertNull(pong.getRanges());
        assertNull(pong.getAltLocs());
        assertNotNull(pong.getPushLocs());
        assertEquals(1, pong.getPushLocs().size());
        Set<? extends IpPort> proxies = pong.getPushLocs().iterator().next().getProxies();
        assertEquals(3, proxies.size());
        // the proxies: 2.3.4.5:5;3.4.5.6:7;4.5.6.7:8
        Set expectedProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"), new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        expectedProxies.retainAll(proxies);
        assertEquals(3, expectedProxies.size());
        for(IpPort ipp : proxies) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                fail("tls capable!: " + ipp);
        }
        
        RemoteFileDesc dummy = 
            new RemoteFileDesc("www.limewire.org", 6346, 10, "asdf", 
                            10, GUID.makeGuid(), 10, true, 2, true, null, 
                               HugeTestUtils.URN_SETS[1],
                               false,false,"",null, -1, false);
        
        Set rfds = pong.getAllLocsRFD(dummy);
        assertEquals(1, rfds.size());
        RemoteFileDesc rfd = (RemoteFileDesc)rfds.toArray()[0]; 
        assertEquals(tlsCollectionPE.getClientGUID(), rfd.getPushAddr().getClientGUID());
        assertEquals(3, rfd.getPushProxies().size());
        Set parsedProxies = new IpPortSet(rfd.getPushProxies());
        parsedProxies.retainAll(expectedProxies);
        assertEquals(3, parsedProxies.size());
        for(IpPort ipp : rfd.getPushProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                fail("tls capable!: " + ipp);
        }        
    }
	
	public void testMixedLocs() throws Exception {
		HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),_havePartial,
				HeadPing.PUSH_ALTLOCS | HeadPing.ALT_LOCS);		
		HeadPong pong = new HeadPong(ping);
        clearStoredProxies();
        pong = reparse(pong);
		
		assertNotNull(pong.getAltLocs());
		assertNotNull(pong.getPushLocs());
		
		RemoteFileDesc rfd = new RemoteFileDesc(
				"1.2.3.4",1,1,"filename",
				1,null,1,
				false,1,false,
				null,null,
				false,false,
				"",
				null,1, false);
		
		Set rfds = pong.getAllLocsRFD(rfd);
		
		assertEquals(pong.getAltLocs().size() + pong.getPushLocs().size(), rfds.size());		
	}
    
    public void testForwardHeadPingDoesntChangeVersion() throws Exception {
        HeadPing ping = new HeadPing(_tlsURN);
        ping = reversion(ping, (short)(HeadPing.VERSION+5));
        assertEquals(HeadPing.VERSION+5, ping.getVersion()); // control.
        
        HeadPing forwarded = new HeadPing(ping);
        assertEquals(HeadPing.VERSION+5, forwarded.getVersion());
    }
	
	private HeadPong reparse(HeadPong original) throws Exception{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		original.write(baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return (HeadPong) MessageFactory.read(bais);
	}
    
    /** Constructs a new HeadPing exactly the same, but with a different version. */
    private HeadPing reversion(HeadPing ping, short version) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ping.write(baos);
        byte[] out = baos.toByteArray();
        ByteOrder.short2leb(version, out, 29); // location of the version of a VM
        HeadPing p2 = (HeadPing)MessageFactory.read(new ByteArrayInputStream(out));
        assertEquals(version, p2.getVersion());
        return p2;
    }
	
	private void  createCollections() throws Exception{
		for(int i=0;i<10;i++ ) {
            AlternateLocation al = AlternateLocation.create("1.2.3."+i+":1234",_haveFull);
            RouterService.getAltlocManager().add(al, null);
		}
        AlternateLocationCollection col = RouterService.getAltlocManager().getDirect(_haveFull);
        assertEquals("failed to set test up", 10, col.getAltLocsSize());
        

        for(int i=0;i<10;i++ ) {
            AlternateLocation al = AlternateLocation.create("1.2.3."+i+":1234",_havePartial);
            RouterService.getAltlocManager().add(al, null);
		}
        col = RouterService.getAltlocManager().getDirect(_havePartial);
        assertEquals("failed to set test up", 10, col.getAltLocsSize());
        
        
        //add some push altlocs to the incomplete collection        
        GUID guid = new GUID(GUID.makeGuid());		
        PushAltLoc firewalled = (PushAltLoc)AlternateLocation.create(guid.toHexString()+";1.2.3.4:5",_havePartial);
        firewalled.updateProxies(true);
		pushCollectionPE = firewalled.getPushAddress();
        RouterService.getAltlocManager().add(firewalled, null);
        col = RouterService.getAltlocManager().getPushNoFWT(_havePartial);
        assertEquals(1, col.getAltLocsSize());
        
        GUID g = new GUID();
        PushAltLoc tls = (PushAltLoc)AlternateLocation.create(g.toHexString() + ";pptls=6;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", _tlsURN);
        tls.updateProxies(true);
        tlsCollectionPE = tls.getPushAddress();
        RouterService.getAltlocManager().add(tls, null);
        col = RouterService.getAltlocManager().getPushNoFWT(_tlsURN);
        assertEquals(1, col.getAltLocsSize());
	}
	
}
