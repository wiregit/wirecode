package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class CoreFoundationWrapper implements CoreFoundation {
	private final CoreFoundation coreFoundation;
	public CoreFoundationWrapper() {
		coreFoundation = (CoreFoundation) Native.loadLibrary("CoreFoundation", CoreFoundation.class);
	}
	

	public String strerror(int errnum) {
		return coreFoundation.strerror(errnum);
	}
	
	public String getLastError() {
		return strerror(Native.getLastError());
	}


	public Pointer CFArrayCreate(Pointer allocator, Pointer[] values,
			int numValues, CFArrayCallBacks callBacks) {
		return coreFoundation.CFArrayCreate(allocator, values, numValues, callBacks);
	}

	public Pointer CFStringCreateWithCString(Pointer allocator, String string,
			int encoding) {
		return coreFoundation.CFStringCreateWithCString(allocator, string, encoding);
	}


	public Pointer CFStringCreateWithCString(String string) {
		return CFStringCreateWithCString(null, string, 0x08000100);
	}


	public Pointer CFRunLoopGetCurrent() {
		return coreFoundation.CFRunLoopGetCurrent();
	}


	public void CFRunLoopRun() {
		coreFoundation.CFRunLoopRun();
		
	}


	public Pointer CFRetain(Pointer pointer) {
		return coreFoundation.CFRetain(pointer);
	}
	
	
}
