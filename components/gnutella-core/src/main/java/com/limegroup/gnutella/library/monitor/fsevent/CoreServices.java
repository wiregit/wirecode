package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface CoreServices extends Library {

	public int FSEventsGetCurrentEventId();
	
	public Pointer FSEventStreamCreate(Pointer allocator, FSEventStreamCallback callback, FSEventStreamContext context, Pointer pathsToWatch, int sinceWhen, double latency, int flags);
	
	public boolean FSEventStreamStart(Pointer streamRef);
	
	public void FSEventStreamScheduleWithRunLoop(Pointer streamRef, Pointer runLoop, Pointer runLoopMod);
	
	public void FSEventStreamFlushSync(Pointer streamRef);
}
