package com.limegroup.gnutella.gui.library;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        assertEquals(Collections.emptySet(), panel.retainAncestors(new File[0]));
    }
    
    public void testRetainAncestorsSingleFile() {
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        File file = new File("/test/file");
        assertEquals(Collections.singleton(file), panel.retainAncestors(file));
    }
    
    public void testRetainAncestorsNoAncestors() {
        File[] files = new File[] {
                new File("/test/test/test"),
                new File("/test/test/hello"),
                new File("/test/dir"),
                new File("/test/tmp/dir"),
                new File("/blah")
        };
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        assertEquals(new HashSet<File>(Arrays.asList(files)), panel.retainAncestors(files));
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
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        assertEquals(new HashSet<File>(Arrays.asList(resulting)), panel.retainAncestors(files));
    }

}
