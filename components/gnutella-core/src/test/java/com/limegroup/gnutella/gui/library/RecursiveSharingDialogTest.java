package com.limegroup.gnutella.gui.library;

import java.awt.Frame;
import java.io.File;
import java.util.Arrays;

import junit.framework.Test;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog.State;

public class RecursiveSharingDialogTest extends GUIBaseTestCase {

	public RecursiveSharingDialogTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(RecursiveSharingDialogTest.class);
    }
	
	public void testShowRecursiveDialogWithEmptyDirs() {
		File[] dirs = LimeTestUtils.createTmpDirs("emptydir");
		RecursiveSharingDialog dialog = new RecursiveSharingDialog((Frame)null, dirs);
		assertEquals(State.OK, dialog.showChooseDialog(null));
		assertTrue(dialog.getRootsToShare().containsAll(Arrays.asList(dirs)));
		assertTrue(dialog.getFoldersToExclude().isEmpty());
	}
}
