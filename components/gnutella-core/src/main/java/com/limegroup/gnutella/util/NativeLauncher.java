package com.limegroup.gnutella.util;

public class NativeLauncher {
	public NativeLauncher() {
		System.loadLibrary("NativeLauncher");
	}

	public void launchWindowsFile(String name) {
		nativeLaunchWindowsFile(name);
	}

	private native void nativeLaunchWindowsFile(String name);
}
