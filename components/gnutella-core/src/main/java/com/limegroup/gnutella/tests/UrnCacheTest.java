package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*; 
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import junit.framework.*;
import junit.extensions.*;

/**
 * Tests the public methods of the UrnCache class.
 */
public final class UrnCacheTest extends TestCase {

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String URN_CACHE_FILE = "fileurns.cache";


	/**
	 * Constructs a new UrnCacheTest with the specified name.
	 */
	public UrnCacheTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(UrnCacheTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void tearDown() {
		UrnCache.instance().persistCache();
	}

	/**
	 * Make sure that the cache contains the expected values.
	 */
  	public void testPersistence() {
//  		assertTrue("cache should be present", cacheExists());
//  		UrnCache cache = UrnCache.instance();
//  		File[] files = createFiles();
//  		for(int i=0; i<files.length; i++) {
//  			assertTrue("file should be present in cache", cache.getUrns(files[i]) != 
//  					   Collections.EMPTY_SET); 
//  			Set set = cache.getUrns(files[i]);
//  			assertTrue("URN set should not be empty", !set.isEmpty());
//  		}
  	}


	private static File[] createFiles() {
		File dir = new File("S:\\Gnutella\\installers\\LimeWire210\\winNoVM");
		return dir.listFiles();
	}


	private static FileDesc[] createFileDescs() {
		File[] files = createFiles();
		FileDesc[] fileDescs = new FileDesc[files.length];
		for(int i=0; i<files.length; i++) {
			fileDescs[i] = new FileDesc(files[i], i);
		}				
		return fileDescs;
	}

	private static void deleteCacheFile() {
		File cacheFile = new File(URN_CACHE_FILE);
		cacheFile.delete();
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private static boolean cacheExists() {
		File cacheFile = new File(URN_CACHE_FILE);
		return cacheFile.exists();
	}

}

