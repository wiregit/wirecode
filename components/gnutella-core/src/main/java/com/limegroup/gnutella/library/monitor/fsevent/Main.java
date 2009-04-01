package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;



public class Main {
	public static void main(String[] args) {
		CoreServices coreServices = new CoreServicesWrapper();
		CoreFoundation coreFoundation = new CoreFoundationWrapper();
		
		System.out.println(coreFoundation.getLastError());
		
		int startId = coreServices.FSEventsGetCurrentEventId();//can also use constant FSEventStreamEventIdSinceNow
		System.out.println(coreFoundation.getLastError());
		System.out.println("startId: " + startId);
		
		

		Pointer allocator= NativeLibrary.getInstance("CoreFoundation").getGlobalVariableAddress("kCFAllocatorDefault").getPointer(0);
		
		Pointer pathsToWatch = coreFoundation.CFArrayCreate(allocator, new Pointer[] {coreFoundation.CFStringCreateWithCString("/Users/pvertenten/test1")}, 1, null);
		System.out.println(coreFoundation.getLastError());
		System.out.println(pathsToWatch);
		FSEventStreamCallback callback = new FSEventStreamCallback() {
			public void callback(Pointer streamRef,
					Pointer clientCallbackInfo, int numEvents,
					Pointer eventPaths, Pointer eventFlags, Pointer eventIds) {
				int[] myEventFlags = eventFlags.getIntArray(0, numEvents);
				int[] myEventIds = eventIds.getIntArray(0, numEvents);
				
				Pointer[] myPaths = eventPaths.getPointerArray(0, numEvents);
				for(Pointer pointer : myPaths) {
					String path = pointer.getString(0);
					System.out.println(path);
				}
				System.out.println("in callback");
				System.out.println("numEvents: " + numEvents);
				
				
				
			}
		};
		FSEventStreamContext context = null;//null is allowed
		
		int flags = 2;
		
		Pointer fEventStreamRef = coreServices.FSEventStreamCreate(allocator, callback, context, pathsToWatch, startId, 1.0, flags);
		System.out.println(coreFoundation.getLastError());
		System.out.println(fEventStreamRef);
		
		Pointer runLoopMode = NativeLibrary.getInstance("CoreFoundation").getGlobalVariableAddress("kCFRunLoopDefaultMode").getPointer(0);
		System.out.println(coreFoundation.getLastError());
		Pointer runLoopGetCurrent = coreFoundation.CFRetain(coreFoundation.CFRunLoopGetCurrent());
		
		
		coreServices.FSEventStreamScheduleWithRunLoop(fEventStreamRef, runLoopGetCurrent, runLoopMode);
		System.out.println(coreFoundation.getLastError());
		
		
		boolean started = coreServices.FSEventStreamStart(fEventStreamRef);
		
		System.out.println(coreFoundation.getLastError());
		System.out.println(started);
		
		coreServices.FSEventStreamFlushSync(fEventStreamRef);
		System.out.println(coreFoundation.getLastError());
		
		coreFoundation.CFRunLoopRun();
		System.out.println(coreFoundation.getLastError());
		
		coreServices.FSEventStreamFlushSync(fEventStreamRef);
		System.out.println(coreFoundation.getLastError());
		
		
		coreServices.FSEventStreamFlushSync(fEventStreamRef);
		System.out.println(coreFoundation.getLastError());
		
		
		System.out.println("done!");
		
		//TODO refactor to file monitor interface.
		//TODO close streams etc.
	
	}

}
