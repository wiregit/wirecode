
package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.downloader.Interval;
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
	static IntervalSet _ranges, _rangesTooBig;
	
	
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
		
		Random r = new Random();
		_ranges = new IntervalSet();
		for (int i=2;i<10;i++) {
			int low = r.nextInt(1000);
			_ranges.add(new Interval(low,low+i));
		}
		
		_rangesTooBig = new IntervalSet();
		for (int i=2;i<200;i++) {
			int low = r.nextInt(1000);
			_rangesTooBig.add(new Interval(low,low+i));
		}
		
		_haveFull = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
		_notHave = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFC");
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
	
	public void testSetUp() {}
}
