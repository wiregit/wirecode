package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class CoreServicesWrapper implements CoreServices{
	private final CoreServices coreServices;
	
	public CoreServicesWrapper() {
		coreServices = (CoreServices) Native.loadLibrary("CoreServices", CoreServices.class);
	}

	public Pointer FSEventStreamCreate(Pointer allocator,
			FSEventStreamCallback callback, FSEventStreamContext context,
			Pointer pathsToWatch, int sinceWhen, double latency, int flags) {
		return coreServices.FSEventStreamCreate(allocator, callback, context, pathsToWatch, sinceWhen, latency, flags);
	}

	public int FSEventsGetCurrentEventId() {
		return coreServices.FSEventsGetCurrentEventId();
	}

	public boolean FSEventStreamStart(Pointer streamRef) {
		return coreServices.FSEventStreamStart(streamRef);
	}

	public void FSEventStreamScheduleWithRunLoop(Pointer streamRef,
			Pointer runLoop, Pointer runLoopMod) {
		coreServices.FSEventStreamScheduleWithRunLoop(streamRef, runLoop, runLoopMod);
		
	}

	public void FSEventStreamFlushSync(Pointer streamRef) {
		coreServices.FSEventStreamFlushSync(streamRef);
		
	}
}
