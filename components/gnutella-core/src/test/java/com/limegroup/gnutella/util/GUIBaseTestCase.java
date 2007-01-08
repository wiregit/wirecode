package com.limegroup.gnutella.util;

import java.io.File;

/**
 * Base class for gui tests that rely on the "gui" folder to be the
 * working directory.
 * 
 * Subclasses must provide a suite method for this setting to work.
 */
public class GUIBaseTestCase extends LimeTestCase {

	private static String savedWorkingDir;
	
	public GUIBaseTestCase(String name) {
		super(name);
	}

	public static void globalSetUp() throws Exception {
		savedWorkingDir = System.getProperty("user.dir");
		File guiDir = getGUIDir();
		System.setProperty("user.dir", guiDir.getAbsolutePath());
	}
	
	public static void globalTearDown() {
		System.setProperty("user.dir", savedWorkingDir);
	}
	
}
