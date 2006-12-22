package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.limewire.util.CommonUtils;

import junit.framework.Test;


/**
 * Tests the public methods of the UrnCache class.
 */
public final class UrnCacheTest extends com.limegroup.gnutella.util.LimeTestCase {

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String URN_CACHE_FILE = "fileurns.cache";
    private static final String FILE_PATH = "com/limegroup/gnutella/util";

    private static final Set EMPTY_SET = 
        Collections.unmodifiableSet(new HashSet());
    
	/**
	 * Constructs a new UrnCacheTest with the specified name.
	 */
	public UrnCacheTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UrnCacheTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    /**
     * Test read & write of map
     */
    public void testPersistence() throws Exception {
        assertTrue("cache should not be present", !cacheExists() );
        
        UrnCache cache = UrnCache.instance();
        FileDesc[] descs = createFileDescs();
        assertNotNull("should have some file descs", descs);
        assertGreaterThan("should have some file descs", 0, descs.length);
        assertTrue("cache should still not be present", !cacheExists() );
        cache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for( int i = 0; i < descs.length; i++) {
            Set set = cache.getUrns(descs[i].getFile());
            assertTrue("file should be present in cache",
               !set.equals(EMPTY_SET)
            );
            assertTrue("URN set should not be empty", !set.isEmpty());
            assertEquals("URN set should be same as that in desc",
                descs[i].getUrns(), set);
        }
    }

	private static FileDesc[] createFileDescs() throws Exception {
        File path = CommonUtils.getResourceFile(FILE_PATH);
        File[] files = path.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
		FileDesc[] fileDescs = new FileDesc[files.length];
		for(int i=0; i<files.length; i++) {
			Set urns = calculateAndCacheURN(files[i]);
			fileDescs[i] = new FileDesc(files[i], urns, i);
		}				
		return fileDescs;
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private static boolean cacheExists() {
		File cacheFile = new File(_settingsDir, URN_CACHE_FILE);
		return cacheFile.exists();
	}

}

