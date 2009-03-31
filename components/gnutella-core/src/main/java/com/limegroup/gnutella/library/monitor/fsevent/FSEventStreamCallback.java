/**
 * 
 */
package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface FSEventStreamCallback extends Callback {
	void callback(Pointer streamRef,
			Pointer clientCallbackInfo, int numEvents, Pointer eventPaths,
			Pointer eventFlags, Pointer eventIds);

}