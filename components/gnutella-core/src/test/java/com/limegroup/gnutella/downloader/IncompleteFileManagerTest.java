package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

public class IncompleteFileManagerTest extends TestCase {

    public IncompleteFileManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(IncompleteFileManagerTest.class);
    }

	public void testLegacy() {
        IncompleteFileManager.unitTest();
    }

}
