
package com.limegroup.gnutella.messages.vendor;

import java.io.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.settings.*;
import com.sun.java.util.collections.*;

import java.util.Random;

import junit.framework.Test;

/**
 * this class tests the handling of udp head requests and responses.
 */
public class UDPHeadTest extends BaseTestCase {

	/**
	 * keep a file manager which shares one complete file and one incomplete file
	 */
	static FileManagerStub _fm;
	static UploadManagerStub _um;
	
	/**
	 * two collections of altlocs, one for the complete and one for the incomplete file
	 */
	static AlternateLocationCollection _alCollectionComplete,_alCollectionIncomplete;
	
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
	
	
	public UDPHeadTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(UDPHeadTest.class);
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
		
		
		Map urns = new HashMap();
		urns.put(_havePartial,_partial);
		urns.put(_haveFull,_complete);
		List descs = new LinkedList();
		descs.add(_partial);
		descs.add(_complete);
		
		_fm = new FileManagerStub(urns,descs);
		
		assertEquals(_partial,_fm.getFileDescForUrn(_havePartial));
		assertEquals(_complete,_fm.getFileDescForUrn(_haveFull));
		
		PrivilegedAccessor.setValue(UDPHeadPong.class, "_fileManager",_fm);
		PrivilegedAccessor.setValue(UDPHeadPong.class, "_uploadManager",_um);
		
	}
	
	/**
	 * tests the scenario where the file cannot be found.
	 */
	public void testFileNotFound() throws Exception {
		UDPHeadPing ping = new UDPHeadPing(_notHave);
		UDPHeadPong pong = reparse(new UDPHeadPong(ping));
		
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
		
		UDPHeadPing ping = new UDPHeadPing(_haveFull);
		UDPHeadPing pingi = new UDPHeadPing(_havePartial);
		UDPHeadPong pong = reparse(new UDPHeadPong(ping));
		UDPHeadPong pongi = reparse(new UDPHeadPong(pingi));
		
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
		
		ping = new UDPHeadPing(_haveFull);
		pingi = new UDPHeadPing(_havePartial);
		pong = reparse(new UDPHeadPong(ping));
		pongi = reparse(new UDPHeadPong(pingi));
		
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
	 * tests requesting ranges from complete, incomplete files
	 * as well as requesting too big ranges to fit in packet.
	 */
	public void testRanges() throws Exception {
		
		UDPHeadPing ping = new UDPHeadPing(_haveFull,UDPHeadPing.INTERVALS);
		UDPHeadPing pingi = new UDPHeadPing(_havePartial,UDPHeadPing.INTERVALS);
		
		UDPHeadPong pong = reparse(new UDPHeadPong(ping));
		UDPHeadPong pongi = reparse(new UDPHeadPong(pingi));
		
		assertTrue(pong.hasCompleteFile());
		assertFalse(pongi.hasCompleteFile());
		
		assertNull(pong.getRanges());
		assertNotNull(pongi.getRanges());
		
		assertTrue(Arrays.equals(_ranges.toBytes(),pongi.getRanges().toBytes()));
		
		//now make the incomplete file desc carry ranges which are too big to
		//fit in a packet
		_partial.setRangesByte(_rangesTooBig.toBytes());
		
		pingi = new UDPHeadPing(_haveFull,UDPHeadPing.INTERVALS);
		pongi = reparse(new UDPHeadPong(pingi));
		
		assertNull(pongi.getRanges());
		assertLessThan(512,pongi.getPayload().length);
		
		_partial.setRangesByte(_ranges.toBytes());
	}
	
	/**
	 * tests various values for the queue rank
	 */
	public void testQueueStatus() throws Exception {
		UDPHeadPing ping = new UDPHeadPing(_havePartial);
		
		UDPHeadPong pong = reparse(new UDPHeadPong(ping));
		
		int allFree =  pong.getQueueStatus();
		assertLessThan(0,allFree);
		
		_um.setUploadsInProgress(10);
		pong = reparse(new UDPHeadPong(ping));
		assertEquals(allFree+10,pong.getQueueStatus());
		
		_um.setNumQueuedUploads(5);
		pong = reparse(new UDPHeadPong(ping));
		assertEquals(5,pong.getQueueStatus());
		
		_um.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue());
		_um.setNumQueuedUploads(0);
		pong = reparse(new UDPHeadPong(ping));
		assertEquals(0,pong.getQueueStatus());
		
		_um.setIsBusy(true);
		_um.setNumQueuedUploads(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
		pong = reparse(new UDPHeadPong(ping));
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
		UDPHeadPing ping1 = new UDPHeadPing(_haveFull,UDPHeadPing.ALT_LOCS);
		UDPHeadPing ping2 = new UDPHeadPing(_havePartial,
				UDPHeadPing.ALT_LOCS | UDPHeadPing.INTERVALS);
		
		UDPHeadPong pong1 = reparse (new UDPHeadPong(ping1));
		UDPHeadPong pong2 = reparse (new UDPHeadPong(ping2));
		
		assertNull(pong1.getRanges());
		assertNotNull(pong2.getRanges());
		assertTrue(Arrays.equals(_rangesMedium.toBytes(),pong2.getRanges().toBytes()));
		assertGreaterThan(pong1.getPayload().length,pong2.getPayload().length);
		
		assertLessThan(pong1.getAltLocs().size(),pong2.getAltLocs().size());
		assertLessThan(512,pong2.getPayload().length);
	}
	
	private UDPHeadPong reparse(UDPHeadPong original) throws Exception{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		original.write(baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return (UDPHeadPong) UDPHeadPong.read(bais);
	}
	
	private static void  createCollections() throws Exception{
			
		Set alternateLocations = new HashSet();
        
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
             alternateLocations.add(
                       AlternateLocation.create(HugeTestUtils.EQUAL_URLS[i]));
		}


        boolean created = false;
		Iterator iter = alternateLocations.iterator();
		for(; iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next();
			if(!created) {
				_alCollectionComplete = 
					AlternateLocationCollection.create(al.getSHA1Urn());
                created = true;
			}            
			_alCollectionComplete.add(al);
		}
        assertTrue("failed to set test up",_alCollectionComplete.getAltLocsSize()==alternateLocations.size());
        
        alternateLocations = new HashSet();
        
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
             alternateLocations.add(
                       AlternateLocation.create(HugeTestUtils.EQUAL_URLS[i]));
		}


        created = false;
		iter = alternateLocations.iterator();
		for(; iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next();
			if(!created) {
				_alCollectionIncomplete = 
					AlternateLocationCollection.create(al.getSHA1Urn());
                created = true;
			}            
			_alCollectionIncomplete.add(al);
		}

        assertTrue("failed to set test up",_alCollectionIncomplete.getAltLocsSize()==alternateLocations.size());
	}
	
}
