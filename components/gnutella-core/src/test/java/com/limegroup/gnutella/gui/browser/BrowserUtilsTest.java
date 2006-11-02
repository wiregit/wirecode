package com.limegroup.gnutella.gui.browser;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class BrowserUtilsTest extends BaseTestCase {

	private File tmpDir;
	
	
	public BrowserUtilsTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		File file = File.createTempFile("magnet", "test");
		tmpDir = file.getParentFile();
		System.setProperty("user.home", tmpDir.getAbsolutePath());
	}
	
	public void testTmpDirSet() {
		assertEquals(tmpDir.getAbsolutePath(), System.getProperty("user.home"));
		assertEquals(tmpDir.getAbsolutePath(), CommonUtils.getUserHomeDir().getAbsolutePath());
	}
	
	public void testDoWriteLimePreferences() throws IOException {
		File prefsFile = BrowserUtils.getPreferencesFile();
		prefsFile.delete();

		BrowserUtils.doWriteLimePreferences();
		assertTrue(prefsFile.exists());
		
		// delete file and test on empty existing file
		assertTrue(prefsFile.delete());
		assertTrue(prefsFile.createNewFile());
		assertEquals(0, prefsFile.length());
		BrowserUtils.doWriteLimePreferences();
		assertTrue(prefsFile.length() > 0);
		
		String contents = new String(FileUtils.readFileFully(prefsFile));
		assertTrue(contents.contains(BrowserUtils.MAGNET_KEY));
		assertTrue(contents.contains(BrowserUtils.UNIX_PREFS));
		assertTrue(contents.contains("limewire"));
		
		// idempotence
		long size = prefsFile.length();
		BrowserUtils.doWriteLimePreferences();
		assertEquals(size, prefsFile.length());

		prefsFile.delete();
	}
}
