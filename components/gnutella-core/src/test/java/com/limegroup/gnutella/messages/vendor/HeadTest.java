package com.limegroup.gnutella.messages.vendor;

import java.io.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.*;
import com.sun.java.util.collections.*;

import java.util.Random;

import junit.framework.Test;

/**
 * this class tests the handling of udp head requests and responses.
 */
public class HeadTest extends BaseTestCase {

	/**
	 * keep a file manager which shares one complete file and one incomplete file
	 */
	static FileManagerStub _fm;
	static UploadManagerStub _um;
	static ConflictsDM _dm;
	
	/**
	 * two collections of altlocs, one for the complete and one for the incomplete file
	 */
	static AlternateLocationCollection _alCollectionComplete,_alCollectionIncomplete,
		_pushCollection;
	
	/**
	 * URNs for the 3 files that will be requested
	 */
	static URN _haveFull,_notHave,_havePartial;
	
	/**
	 * file descs for the partial and complete files
	 */
	static IncompleteFileDescStub _partial;
	static FileDescStub _complete;
	/**
	 * an interval that can fit in a packet, and one that can't
	 */
	static IntervalSet _ranges, _rangesMedium, _rangesTooBig;
	
	static PushEndpoint pe;
	
	
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
		//PrivilegedAccessor.setValue(RouterService.class,"acceptor", new AcceptorStub());
		_fm = new FileManagerStub();
		_um = new UploadManagerStub();
		_dm = new ConflictsDM();
		
		int base=0;
		_ranges = new IntervalSet();
		for (int i=2;i<10;i++) {
			int low = base;
			_ranges.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesMedium = new IntervalSet();
		for (int i=2;i<63;i++) {
			int low = base;
			_rangesMedium.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		base=0;
		_rangesTooBig = new IntervalSet();
		for (int i=2;i<200;i++) {
			int low = base;
			_rangesTooBig.add(new Interval(low,low+i));
			base+=2*i;
		}
		
		_haveFull = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
		_notHave = FileManagerStub._notHave;
		_havePartial = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFD");
		
		createCollections();
		
		_partial = new IncompleteFileDescStub("incomplete",_havePartial,3);
		_partial.setRangesByte(_ranges.toBytes());
		_complete = new FileDescStub("complete",_haveFull,2);
		
		_complete.setAlternateLocationCollection(_alCollectionComplete);
		_partial.setAlternateLocationCollection(_alCollectionIncomplete);
		_partial.setPushAlternateLocationCollection(_pushCollection);
		
		
		
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
		
	}
	
	/**
	 * tests the scenario where the file cannot be found.
	 */
	public void testFileNotFound() throws Exception {
		HeadPing ping = new HeadPing(_notHave);
		HeadPong pong = reparse(new HeadPong(ping));
		
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
	public void testFirewalled() throws Exception {
		Acceptor acceptor = RouterService.getAcceptor();
		PrivilegedAccessor.setValue(acceptor,"_acceptedIncoming", new Boolean(false));
		assertFalse(RouterService.acceptedIncomingConnection());
		
		HeadPing ping = new HeadPing(_haveFull);
		HeadPing pingi = new HeadPing(_havePartial);
		HeadPong pong = reparse(new HeadPong(ping));
		HeadPong pongi = reparse(new HeadPong(pingi));
		
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
		
		PrivilegedAccessor.setValue(acceptor,"_acceptedIncoming", new Boolean(true));
		assertTrue(RouterService.acceptedIncomingConnection());
		
		ping = new HeadPing(_haveFull);
		pingi = new HeadPing(_havePartial);
		pong = reparse(new HeadPong(ping));
		pongi = reparse(new HeadPong(pingi));
		
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
	public void testDownloading() throws Exception {
		
		//replace the downloadManger with a stub
		Object originalDM = RouterService.getDownloadManager();
		
		PrivilegedAccessor.setValue(RouterService.class,"downloader",_dm);
		_dm._conflicts.add(_havePartial);
		
		HeadPing ping = new HeadPing(_havePartial);
		HeadPong pong = reparse (new HeadPong(ping));
		
		assertTrue(pong.isDownloading());
		
		_dm._conflicts.clear();
		
		pong = reparse (new HeadPong(ping));
		
		assertFalse(pong.isDownloading());
		
		//restore the original download manager
		PrivilegedAccessor.setValue(RouterService.class,"downloader",originalDM);
	}
	/**
	 * tests requesting ranges from complete, incomplete files
	 * as well as requesting too big ranges to fit in packet.
	 */
	public void testRanges() throws Exception {
		
		HeadPing ping = new HeadPing(_haveFull,HeadPing.INTERVALS);
		HeadPing pingi = new HeadPing(_havePartial,HeadPing.INTERVALS);
		
		HeadPong pong = reparse(new HeadPong(ping));
		HeadPong pongi = reparse(new HeadPong(pingi));
		
		assertTrue(pong.hasCompleteFile());
		assertFalse(pongi.hasCompleteFile());
		
		assertNull(pong.getRanges());
		assertNotNull(pongi.getRanges());
		
		assertTrue(Arrays.equals(_ranges.toBytes(),pongi.getRanges().toBytes()));
		
		//now make the incomplete file desc carry ranges which are too big to
		//fit in a packet
		_partial.setRangesByte(_rangesTooBig.toBytes());
		
		pingi = new HeadPing(_havePartial,HeadPing.INTERVALS);
		pongi = reparse(new HeadPong(pingi));
		
		assertNull(pongi.getRanges());
		assertLessThan(512,pongi.getPayload().length);
		
		_partial.setRangesByte(_ranges.toBytes());
	}
	
	/**
	 * tests various values for the queue rank
	 */
	public void testQueueStatus() throws Exception {
		HeadPing ping = new HeadPing(_havePartial);
		
		HeadPong pong = reparse(new HeadPong(ping));
		
		int allFree =  pong.getQueueStatus();
		assertLessThan(0,allFree);
		
		_um.setUploadsInProgress(10);
		pong = reparse(new HeadPong(ping));
		assertEquals(allFree+10,pong.getQueueStatus());
		
		_um.setNumQueuedUploads(5);
		pong = reparse(new HeadPong(ping));
		assertEquals(5,pong.getQueueStatus());
		
		_um.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue());
		_um.setNumQueuedUploads(0);
		pong = reparse(new HeadPong(ping));
		assertEquals(0,pong.getQueueStatus());
		
		_um.setIsBusy(true);
		_um.setNumQueuedUploads(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
		pong = reparse(new HeadPong(ping));
		assertGreaterThanOrEquals(127,pong.getQueueStatus());
	}
	
	/**
	 * tests handling of alternate locations.
	 */
	public void testAltLocs() throws Exception {
		
		//add some big interval that fill most of the packet but not all
		_partial.setRangesByte(_rangesMedium.toBytes());
		
		//ping 1 should contain alternate locations. 
		
		//the second ping should be too big to contain all altlocs.
		HeadPing ping1 = new HeadPing(_haveFull,HeadPing.ALT_LOCS);
		HeadPing ping2 = new HeadPing(_havePartial,
				HeadPing.ALT_LOCS | HeadPing.INTERVALS);
		
		HeadPong pong1 = reparse (new HeadPong(ping1));
		HeadPong pong2 = reparse (new HeadPong(ping2));
		
		assertNull(pong1.getRanges());
		assertNotNull(pong2.getRanges());
		assertTrue(Arrays.equals(_rangesMedium.toBytes(),pong2.getRanges().toBytes()));
		assertGreaterThan(pong1.getPayload().length,pong2.getPayload().length);
		
		assertLessThan(pong1.getAltLocs().size(),pong2.getAltLocs().size());
		assertLessThan(512,pong2.getPayload().length);
	}
	
	public void testFirewalledAltlocs() throws Exception {
		
		//try with a file that doesn't have push locs
		HeadPing ping1 = new HeadPing(_haveFull,HeadPing.PUSH_ALTLOCS);
		assertTrue(ping1.requestsPushLocs());
		HeadPong pong1 = reparse (new HeadPong(ping1));
		assertNull(pong1.getPushLocs());
		
		ping1 = new HeadPing(_havePartial,HeadPing.PUSH_ALTLOCS);
		assertTrue(ping1.requestsPushLocs());
		pong1 = reparse (new HeadPong(ping1));

		assertNull(pong1.getRanges());
		assertNull(pong1.getAltLocs());
		assertNotNull(pong1.getPushLocs());
		
		RemoteFileDesc dummy = 
			new RemoteFileDesc("www.limewire.org", 6346, 10, HTTPConstants.URI_RES_N2R+
							   HugeTestUtils.URNS[1].httpStringValue(), 10, 
							   GUID.makeGuid(), 10, true, 2, true, null, 
							   HugeTestUtils.URN_SETS[1],
                               false,false,"",0,null, -1);
		
		Set received = pong1.getAllLocsRFD(dummy);
		assertEquals(1,received.size());
		RemoteFileDesc rfd = (RemoteFileDesc)received.toArray()[0]; 
		PushEndpoint point = rfd.getPushAddr();
		assertEquals(pe,point);
	}
	
	private HeadPong reparse(HeadPong original) throws Exception{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		original.write(baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return (HeadPong) HeadPong.read(bais);
	}
	
	private static void  createCollections() throws Exception{
			
		


        _alCollectionComplete=AlternateLocationCollection.create(_haveFull);
        _alCollectionIncomplete=AlternateLocationCollection.create(_havePartial);
        _pushCollection=AlternateLocationCollection.create(_havePartial);
		
		for(int i=0;i<10;i++ ) {
            AlternateLocation al = AlternateLocation.create("1.2.3."+i+":1234",_haveFull);
			_alCollectionComplete.add(al);
		}
        assertEquals("failed to set test up",10,
        		_alCollectionComplete.getAltLocsSize());
        
        for(int i=0;i<10;i++ ) {
            AlternateLocation al = AlternateLocation.create("1.2.3."+i+":1234",_havePartial);
			_alCollectionIncomplete.add(al);
		}
        assertEquals("failed to set test up",10,
        		_alCollectionIncomplete.getAltLocsSize());
        

        
        //add some firewalled altlocs to the incomplete collection
        
        GUID guid = new GUID(GUID.makeGuid());
		
		AlternateLocation firewalled = AlternateLocation.create(guid.toHexString()+
				";1.2.3.4:5",_havePartial);
		pe = ((PushAltLoc)firewalled).getPushAddress();
		_pushCollection=AlternateLocationCollection.create(_havePartial);
		_pushCollection.add(firewalled);
	}
	
	
	static class ConflictsDM extends DownloadManagerStub {
		public Set _conflicts= new HashSet();
		public boolean conflicts(URN urn) {
			return _conflicts.contains(urn);
		}
	}
}
