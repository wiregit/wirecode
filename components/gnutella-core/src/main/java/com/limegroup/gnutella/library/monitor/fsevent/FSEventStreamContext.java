/**
 * 
 */
package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class FSEventStreamContext extends Structure {
	public int version;
	public Pointer info;
	public CFAllocationRetainCallback retain;
	public CFAllocationReleaseCallabk release;
	public CFAllocatorCopyDescriptionCallBack copyDescription;
}