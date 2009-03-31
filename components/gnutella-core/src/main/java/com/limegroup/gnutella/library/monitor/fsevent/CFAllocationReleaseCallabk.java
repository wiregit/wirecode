/**
 * 
 */
package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface CFAllocationReleaseCallabk extends Callback {
	void callback(Pointer info);
}