
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;

import junit.framework.Test;

/**
 * This test tests the logic in the <tt>Connection</tt> class which
 * handles BestCandidatesMessages
 * 
 * There are several scenarios:
 * 
 * 1.  We have never sent a message on this connection before
 *     and are sending a new one.
 * 2.  We have never sent a message on this connection before
 *     and are calling it for maintenance.
 * 3.  We have sent a message, the minimum interval has passed
 *     and now we send another message.
 * 4.  We have sent a message, the minimum interval has passed
 *     and now we send the same message.
 * 5.  We have sent a message, the minimun interval has passed
 *     and now we are calling it for maintenance, no update needed.
 * 6.  We have sent a message, the minimun interval has passed
 *     and now we are calling it for maintenance, update is needed.
 * 7.  The minimum interval has not passed and we are calling to send
 *     a new message. (twice)
 * 8.  The minimum interval has not passed and we are calling 
 *     to send the same message.
 * 9.  The minimum interval has not passed, no update needed and we are calling
 * 	   for maintenance.
 * 10.  The minimum interval has not passed, update is needed and we are calling
 * 	   for maintenance.
 */
public class BCVMHandlingTest extends BaseTestCase {
	
	static long NEW_INTERVAL = 300;
	
	static TestConnection _connection; 
	static BestCandidatesVendorMessage _bcvm, _bcvm2;
	
	public BCVMHandlingTest(String str){
		super(str);
	}
	
	public static Test suite() {
        return buildTestSuite(BCVMHandlingTest.class);
    }
	
	protected void setUp() {
		_connection = new TestConnection();
		
		
		try {
			
			Candidate [] candidates = new Candidate[2];
			candidates[0] = new RemoteCandidate("1.2.3.4",15,(short)20);
			candidates[1]=null;
			
			_bcvm = new BestCandidatesVendorMessage(candidates);
			
			Candidate [] update = new Candidate[2];
			update[0] = new RemoteCandidate("2.2.2.2",20,(short)20);
			update[1] = null;
			
			_bcvm2 = new BestCandidatesVendorMessage(update);
			
			
			PrivilegedAccessor.setValue(Connection.class,"ADVERTISEMENT_INTERVAL",
					new Long(NEW_INTERVAL));
			
		}catch(Exception e) {
			e.printStackTrace();
			return;
		}
	}
		
	/**
	 *	1.  We have never sent a message on this connection before
	 *      and are sending a new one. 
	 */
	public void testNewAdvertisement() throws Exception {
		
		assertNull(_connection.getLastSent());
		_connection.handleBestCandidatesMessage(_bcvm);
		
		//make sure this message was sent
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		
		//and that the various fields were updated.
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		assertGreaterThan(0,_connection.getLastAdvertisementTime());
	}
	
	/**
	 *  2.  We have never sent a message on this connection before
	 *      and are calling it for maintenance. 
	 */
	public void testNewAdvertisementNull() throws Exception {
		assertNull(_connection.getLastSent());
		assertFalse(_connection.needsAdvertisement());
		assertNull(_connection.getCandidatesSent());
		assertEquals(0,_connection.getLastAdvertisementTime());
		
		//if this connection gets called for maintenance, nothing should have changed.
		_connection.handleBestCandidatesMessage(null);
		
		assertNull(_connection.getLastSent());
		assertFalse(_connection.needsAdvertisement());
		assertNull(_connection.getCandidatesSent());
		assertEquals(0,_connection.getLastAdvertisementTime());
	}
	
	/**
	 * 3.  We have sent a message, the minimum interval has passed
	 *     and now we send another message.
	 */
	public void testSecondAdvertisement() throws Exception {
		
		
		
		assertFalse(_bcvm2.isSame(_bcvm));
		
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//wait the timeout
		Thread.sleep(NEW_INTERVAL+20);
		
		//send the second message
		_connection.handleBestCandidatesMessage(_bcvm2);
		
		//make sure this message was sent
		assertTrue(
				_bcvm2.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		
		//see if the fields were updated
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		assertGreaterThan(firstSent,_connection.getLastAdvertisementTime());
		
	}
	
	/**
	 * 4.  We have sent a message, the minimum interval has passed
	 *     and now we send the same message.
	 */
	public void testSecondAdvertisementSameMessage() throws Exception {
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//wait the timeout
		Thread.sleep(NEW_INTERVAL+20);
		
		//send the first message again
		_connection.handleBestCandidatesMessage(_bcvm);
		
		//see if the fields were updated
		//the message will stay the same
		//there should be no need to re-advertise
		//and the last time we sent a message should not have been
		//updated.
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
	}
	
	/**
	 * 5.  We have sent a message, the minimum interval has passed
	 *     and now we are calling it for maintenance, no update needed.
	 */
	public void testSecondAdvertisementNull() throws Exception {
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//wait the timeout
		Thread.sleep(NEW_INTERVAL+20);
		
		//call for maintenance
		_connection.handleBestCandidatesMessage(null);
		
		//see if the fields were updated
		//the message will stay the same
		//there should be no need to re-advertise
		//and the last time we sent a message should not have been
		//updated.
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
	}
	
	/**
	 * 6.  We have sent a message, the minimum interval has passed
	 *     and now we are calling it for maintenance, update is needed.
	 */
	public void testUpdateNeededCallNull() throws Exception {
		
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//make sure it gets sent
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		
		//wait half of the timeout
		Thread.sleep(NEW_INTERVAL/2);
		
		//now try to send another update
		_connection.handleBestCandidatesMessage(_bcvm2);
		
		//make sure it does not get sent out.
		assertFalse(
				_bcvm2.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
		
		//however it should have been put on the send queue.
		
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent()));
		
		//and the update flag should be set.
		assertTrue(_connection.needsAdvertisement());
	
		//*****************************************
		//sleep some time, but not going beyond the interval
		Thread.sleep(NEW_INTERVAL/4);
		//call for maintenance
		_connection.handleBestCandidatesMessage(null);
		
		//nothing should happen.
		assertTrue(_connection.needsAdvertisement()); //should still need update
		assertFalse( //should not have sent the new message
				_bcvm2.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent())); //the new message should be on queue
		
		//************************************
		//sleep some more time, this time going outside of the interval
		Thread.sleep(NEW_INTERVAL/2);
		
		//call for maintenance
		_connection.handleBestCandidatesMessage(null);
		
		//this maintenance call should send the new message that was on the queue
		//and should clear the update needed flag
		assertFalse(_connection.needsAdvertisement());
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent()));
		assertTrue( 
				_bcvm2.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertGreaterThan(firstSent,_connection.getLastAdvertisementTime());
		
	}
	
	/**
	 * 
	 * 7.  The minimum interval has not passed and we are calling to send
	 *     a new message. (twice)
	 */
	public void testCallTwiceInInterval() throws Exception {
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//make sure it gets sent
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		
		//wait half of the timeout
		Thread.sleep(NEW_INTERVAL/2);
		
		//now try to send another update
		_connection.handleBestCandidatesMessage(_bcvm2);
		
		//make sure it does not get sent out.
		assertFalse(
				_bcvm2.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
		
		//however it should have been put on the send queue.
		
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent()));
		
		//and the update flag should be set.
		assertTrue(_connection.needsAdvertisement());
	
		//*****************************************
		//sleep some time, but not going beyond the interval
		Thread.sleep(NEW_INTERVAL/4);
		
		//clear the last sent message
		_connection.setLastSent(null);
		
		//now try to send the first message again.
		_connection.handleBestCandidatesMessage(_bcvm);

		//make sure nothing gets sent
		assertNull(_connection.getLastSent());
		
		//the update needed flag should still be set
		assertTrue(_connection.needsAdvertisement());
		
		//the last advertisement time should not have changed
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
		
		//but the message on the send queue should be updated.
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
	}
	
	/**
	 * 8.  We have sent a message, the minimum interval has not passed
	 *     but we try to send the same message.
	 */
	public void testInIntervalSameMessage() throws Exception {
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//wait half the timeout
		Thread.sleep(NEW_INTERVAL/2);
		
		//send the first message again
		_connection.handleBestCandidatesMessage(_bcvm);
		
		//see if the fields were updated
		//the message will stay the same
		//there should be no need to re-advertise
		//and the last time we sent a message should not have been
		//updated.
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
	}
	/**
	 * 9.  The minimum interval has not passed, no update needed and we are calling
	 * 			for maintenance.
	 */	   
	public void testInIntervalNullNoUpdate() throws Exception {
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		assertFalse(_connection.needsAdvertisement());
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//wait half the timeout
		Thread.sleep(NEW_INTERVAL/2);
		
		//perform maintenance
		_connection.handleBestCandidatesMessage(null);
		
		//see if the fields were updated
		//the message will stay the same
		//there should be no need to re-advertise
		//and the last time we sent a message should not have been
		//updated.
		assertTrue(_bcvm.isSame(_connection.getCandidatesSent()));
		assertFalse(_connection.needsAdvertisement());
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
	}
	/**
	 * 10.  The minimum interval has not passed, update is needed and we are calling
	 * 	   for maintenance.
	 */
	public void testInIntervalNullUpdate() throws Exception {
		//send the first message
		_connection.handleBestCandidatesMessage(_bcvm);
		assertFalse(_connection.needsAdvertisement());
		
		long firstSent = _connection.getLastAdvertisementTime();
		
		//wait half the timeout
		Thread.sleep(NEW_INTERVAL/2);
		
		//send another message
		_connection.handleBestCandidatesMessage(_bcvm2);
		
		//we should need maintenance now.
		assertTrue(_connection.needsAdvertisement());
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent()));
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
		
		//sleep some more, but not passing the interval
		Thread.sleep(NEW_INTERVAL/4);
		//perform maintenance
		_connection.handleBestCandidatesMessage(null);
		
		//everything should stay the same
		assertTrue(_connection.needsAdvertisement());
		assertTrue(_bcvm2.isSame(_connection.getCandidatesSent()));
		assertEquals(firstSent,_connection.getLastAdvertisementTime());
		assertTrue(
				_bcvm.isSame((BestCandidatesVendorMessage) _connection.getLastSent()));
	}
	
	/**
	 * a utility class with various getters.
	 */
	static class TestConnection extends ManagedConnectionStub {
		
		public BestCandidatesVendorMessage getCandidatesSent() {
			return _candidatesSent;
		}
		
		public long getLastAdvertisementTime() {
			Long last = null;
			try{
				last= (Long)
					PrivilegedAccessor.getValue(this,"_lastSentAdvertisementTime");
			}catch(Exception e) {
				last = new Long(-1);
			}
			return last.longValue();
		}
		
		public boolean needsAdvertisement() {
			Boolean yes = null;
			try{
				yes= (Boolean)
					PrivilegedAccessor.getValue(this,"_needsAdvertisement");
			}catch(Exception e) {
				yes = new Boolean(false);
			}
			return yes.booleanValue();
		}
	}
}
