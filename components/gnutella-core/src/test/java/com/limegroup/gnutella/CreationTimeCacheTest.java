package com.limegroup.gnutella;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import junit.framework.*;
import junit.extensions.*;

/**
 * Tests the public methods of the UrnCache class.
 */
public final class CreationTimeCacheTest 
    extends com.limegroup.gnutella.util.BaseTestCase {

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String CREATION_CACHE_FILE = "createtimes.cache";
    private static final String FILE_PATH = "com/limegroup/gnutella/util";
    private static FileDesc[] descs;

	/**
	 * Constructs a new CreationTimeCacheTest with the specified name.
	 */
	public CreationTimeCacheTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(CreationTimeCacheTest.class);
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
        
        CreationTimeCache cache = CreationTimeCache.instance();
        FileDesc[] descs = createFileDescs();
        assertNotNull("should have some file descs", descs);
        assertGreaterThan("should have some file descs", 0, descs.length);
        assertTrue("cache should still not be present", !cacheExists() );
        cache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for( int i = 0; i < descs.length; i++) {
            Long cTime = cache.getCreationTime(descs[i].getSHA1Urn());
            assertNotNull("file should be present in cache", cTime);
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
			Set urns = FileDesc.calculateAndCacheURN(files[i]);            
			fileDescs[i] = new FileDesc(files[i], urns, i);
            CreationTimeCache.instance().addTime(fileDescs[i].getSHA1Urn(),
                                                 files[i].lastModified());
		}				
		return fileDescs;
	}

	private static void deleteCacheFile() {
		File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
		cacheFile.delete();
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private static boolean cacheExists() {
		File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
		return cacheFile.exists();
	}

}

