package com.limegroup.gnutella.gui.library;

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
	
	public void testRetainAncestorsEmptyInput() {
		assertEquals(new File[0], RecursiveSharingDialog.retainAncestors(new File[0]));
	}
	
	public void testRetainAncestorsSingleFile() {
		File file = new File("/test/file");
		assertEquals(new File[] { file }, RecursiveSharingDialog.retainAncestors(file));
	}
	
	public void testRetainAncestorsNoAncestors() {
		File[] files = new File[] {
				new File("/test/test/test"),
				new File("/test/test/hello"),
				new File("/test/dir"),
				new File("/test/tmp/dir"),
				new File("/blah")
		};
		assertEquals(files, RecursiveSharingDialog.retainAncestors(files));
	}
	
	public void testRetainAncestorsWithSubfoldersToRemove() {
		File[] files = new File[] {
				new File("/test/test/"),
				new File("/test/test/hello"),
				new File("/test/dir"),
				new File("/test/dir"),
				new File("/blah")
		};
		File[] resulting = new File[] {
				new File("/test/test/"),
				new File("/test/dir"),
				new File("/blah")
		};
		assertEquals(resulting, RecursiveSharingDialog.retainAncestors(files));
	}

	public void testShowRecursiveDialogWithEmptyDirs() {
		File[] dirs = LimeTestUtils.createTmpDirs("emptydir");
		RecursiveSharingDialog dialog = new RecursiveSharingDialog(null, dirs);
		assertEquals(State.OK, dialog.showChooseDialog(null));
		assertTrue(dialog.getRootsToShare().containsAll(Arrays.asList(dirs)));
		assertTrue(dialog.getFoldersToExclude().isEmpty());
	}
}
