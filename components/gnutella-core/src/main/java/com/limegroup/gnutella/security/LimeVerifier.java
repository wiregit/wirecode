
package com.limegroup.gnutella.security;

import com.limegroup.gnutella.*;

import java.io.IOException;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.GiveStatsVendorMessage;

import java.util.*;

/**
 * This is a task which closes a connection unless cancelled.
 */
public class LimeVerifier implements Runnable {

	public static int INITIAL_INTERVAL=5*1000; //5 seconds
	public static int RESPONSE_TIME = 15*1000; //10 seconds
	
	/**
	 * the message used to verify if the other side is limewire
	 */
	private static Message _testMessage =
		new GiveStatsVendorMessage((byte)1,(byte)1,Message.N_TCP);
	
	/**
	 * <tt> ManagedConnection -> LimeVerifier map</tt>
	 */
	private static Map _verifiers = Collections.synchronizedMap(new HashMap());
	
	/**
	 * marks the connection as a LimeWire node that has successfully verified itself
	 * @param handler the connection that successfully identified itself
	 */
	public static void clearSuspect(ReplyHandler handler) {
		LimeVerifier cleared = (LimeVerifier) _verifiers.remove(handler);
		cleared.cancel();
	}
	
	
	/**
	 * schedule an authenticity test on the given connection object
	 * @param suspect an initialized <tt>ManagedConnection</tt> object
	 * that should be tested to be authentic LimeWire node
	 */
	public static void registerSuspect(ManagedConnection suspect) {
		LimeVerifier verifier = new LimeVerifier(suspect);
		
		RouterService.schedule(verifier, INITIAL_INTERVAL, 0);
		
		_verifiers.put(suspect, verifier);
	}
	
	/**
	 * whether this task is cancelled
	 */
	private boolean _cancelled;
	
	/**
	 * whether we have already sent the test message and are expecting a reply
	 */
	private boolean _expecting;
	
	/**
	 * the suspected connection
	 */
	private final ManagedConnection _suspect;
	
	private void cancel() {
		_cancelled=true;
	}
	
	private LimeVerifier(ManagedConnection c) {
		_suspect = c;
		_cancelled=false;
		_expecting=false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		if (_cancelled) 
			return;
		
		
		if (!_expecting) {
			
			//send an arbitrary give stats vendor message.
			_suspect.send(_testMessage);
			
			_expecting= true;
			
			RouterService.schedule(this, RESPONSE_TIME,0);
			
			
		} else 
			RouterService.getConnectionManager().remove(_suspect);

	}
	

}
