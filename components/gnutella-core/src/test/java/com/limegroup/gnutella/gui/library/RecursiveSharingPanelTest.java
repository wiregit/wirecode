package com.limegroup.gnutella.gui.library;

import java.io.File;

import junit.framework.Test;

import com.limegroup.gnutella.gui.GUIBaseTestCase;

public class RecursiveSharingPanelTest extends GUIBaseTestCase {
    
    public RecursiveSharingPanelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RecursiveSharingPanelTest.class);
    }
    
    public void testRetainAncestorsEmptyInput() {
        assertEquals(new File[0], RecursiveSharingPanel.retainAncestors(new File[0]));
    }
    
    public void testRetainAncestorsSingleFile() {
        File file = new File("/test/file");
        assertEquals(new File[] { file }, RecursiveSharingPanel.retainAncestors(file));
    }
    
    public void testRetainAncestorsNoAncestors() {
        File[] files = new File[] {
                new File("/test/test/test"),
                new File("/test/test/hello"),
                new File("/test/dir"),
                new File("/test/tmp/dir"),
                new File("/blah")
        };
        assertEquals(files, RecursiveSharingPanel.retainAncestors(files));
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
        assertEquals(resulting, RecursiveSharingPanel.retainAncestors(files));
    }

}
