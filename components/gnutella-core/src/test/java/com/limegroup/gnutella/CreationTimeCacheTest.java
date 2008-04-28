package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.MetaFileManager;

public class CreationTimeCacheTest extends LimeTestCase {
    
    private URN hash1;
    private URN hash2;
    private URN hash3;
    private URN hash4;
    
    private MyFileManager fileManager;
    private Injector injector;

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String CREATION_CACHE_FILE = "createtimes.cache";
    private final String FILE_PATH = "com/limegroup/gnutella/util";

	public CreationTimeCacheTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(CreationTimeCacheTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}      
    
    @Override
    public void setUp() throws Exception  {
        hash1 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSASUSH");
        hash2 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSANITA");
        hash3 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQABOALT");
        hash4 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5BERKELEY");
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
                bind(FileManager.class).to(MyFileManager.class);
                bind(MetaFileManager.class).to(MyFileManager.class);
            } 
        });
        
        fileManager = (MyFileManager)injector.getInstance(FileManager.class);
        fileManager.setDefaultUrn(hash1);
        fileManager.setValidUrns(new HashSet<URN>(Arrays.asList(hash1, hash2, hash3, hash4)));
    }

    ///////////////////////// Actual Tests ////////////////////////////
    
    
    @SuppressWarnings("unchecked")
    private Map<URN, Long> getUrnToTime(CreationTimeCache cache) throws Exception {
        Future<?> future = (Future)PrivilegedAccessor.getValue(cache, "deserializer");
        Object o = future.get();
        return (Map<URN, Long>)PrivilegedAccessor.invokeMethod(o, "getUrnToTime");
    }

    /** Tests that the URN_MAP is derived correctly from the URN_TO_TIME_MAP
     */
    public void testMapCreation() throws Exception {
        // mock up our own createtimes.txt
        Map<URN, Long> toSerialize = new HashMap<URN, Long>();
        Long old = new Long(1);
        Long middle = new Long(2);
        Long young = new Long(3);
        toSerialize.put(hash1, old);
        toSerialize.put(hash2, middle);
        toSerialize.put(hash3, middle);
        toSerialize.put(hash4, young);

        ObjectOutputStream oos = 
        new ObjectOutputStream(new FileOutputStream(new File(_settingsDir,
                                                             CREATION_CACHE_FILE)));
        oos.writeObject(toSerialize);
        oos.close();
        
        // now have the CreationTimeCache read it in
        CreationTimeCache ctCache = new CreationTimeCache(fileManager);
        Map map = getUrnToTime(ctCache);
        assertEquals(toSerialize, map);
    }
    
    public void testMapCreationNoExistingMap() throws Exception {
        CreationTimeCache creationTimeCache = new CreationTimeCache(fileManager);
        Map<URN, Long> map = creationTimeCache.createMap();
        assertTrue(map.isEmpty());
    }


    /** Tests the getFiles().iterator() method.
     */
    public void testGetFiles() throws Exception {
        // mock up our own createtimes.txt
        Map<URN, Long> toSerialize = new HashMap<URN, Long>();
        Long old = new Long(1);
        Long middle = new Long(2);
        Long young = new Long(3);
        toSerialize.put(hash1, middle);
        toSerialize.put(hash2, young);
        toSerialize.put(hash3, old);
        toSerialize.put(hash4, middle);

        ObjectOutputStream oos = 
        new ObjectOutputStream(new FileOutputStream(new File(_settingsDir,
                                                             CREATION_CACHE_FILE)));
        oos.writeObject(toSerialize);
        oos.close();
        
        // now have the CreationTimeCache read it in
        CreationTimeCache ctCache = new CreationTimeCache(fileManager);
        // is everything mapped correctly from URN to Long?
        assertEquals(ctCache.getCreationTime(hash1), middle);
        assertEquals(ctCache.getCreationTime(hash2), young);
        assertEquals(ctCache.getCreationTime(hash3), old);
        assertEquals(ctCache.getCreationTime(hash4), middle);

        {
            Iterator iter = ctCache.getFiles().iterator();
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertEquals(hash3, iter.next());
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(4).iterator();
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertEquals(hash3, iter.next());
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(3).iterator();
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(2).iterator();
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(1).iterator();
            assertEquals(hash2, iter.next());
            assertFalse(iter.hasNext());
        }

        {
            try {
                ctCache.getFiles(0).iterator();
            } catch (IllegalArgumentException expected) {}
        }

    }


    /** Tests the getFiles().iterator() method.
     */
    public void testSettersAndGetters() throws Exception {
        MyFileManager fileManager = (MyFileManager)injector.getInstance(FileManager.class);
        Iterator iter = null;
        Map TIME_MAP = null;
        Long old = new Long(1);
        Long middle = new Long(2);
        Long young = new Long(3);
        deleteCacheFile();
        
        // should be a empty cache
        // ---------------------------
        CreationTimeCache ctCache = new CreationTimeCache(fileManager);
        assertFalse(ctCache.getFiles().iterator().hasNext());

        TIME_MAP = getUrnToTime(ctCache);
        assertEquals(0, TIME_MAP.size());

        ctCache.addTime(hash1, middle.longValue());
        ctCache.commitTime(hash1);
        ctCache.persistCache();
        iter = ctCache.getFiles().iterator();
        assertEquals(hash1, iter.next());
        assertFalse(iter.hasNext());
        ctCache = null;
        // ---------------------------

        // should have one value
        // ---------------------------
        ctCache = new CreationTimeCache(fileManager); // test the deserialization.
        iter = ctCache.getFiles().iterator();
        assertEquals(hash1, iter.next());
        assertFalse(iter.hasNext());

        TIME_MAP = getUrnToTime(ctCache);
        assertEquals(1, TIME_MAP.size());

        ctCache.addTime(hash2, old.longValue());
        ctCache.commitTime(hash2);
        ctCache.addTime(hash3, young.longValue());
        ctCache.commitTime(hash3);
        ctCache.addTime(hash4, middle.longValue());
        ctCache.commitTime(hash4);
        ctCache.removeTime(hash1);
        ctCache.persistCache();
        iter = ctCache.getFiles().iterator();
        assertEquals(hash3, iter.next());
        // just clear middle
        iter.next();
        assertEquals(hash2, iter.next());
        assertFalse(iter.hasNext());
        ctCache = null;
        // ---------------------------

        // should have three values
        // ---------------------------
        ctCache = new CreationTimeCache(fileManager); // test the deserialization.
        iter = ctCache.getFiles().iterator();
        assertEquals(hash3, iter.next());
        assertEquals(hash4, iter.next());
        assertEquals(hash2, iter.next());
        assertFalse(iter.hasNext());

        TIME_MAP = getUrnToTime(ctCache);
        assertEquals(3, TIME_MAP.size());
        ctCache.removeTime(hash3);
        ctCache.persistCache();
        // ---------------------------

        // should have two values but exclude one
        // ---------------------------
        fileManager.setExcludeURN(hash4);
        ctCache = new CreationTimeCache(fileManager); // test the deserialization.
        iter = ctCache.getFiles().iterator();
        assertEquals(hash2, iter.next());
        assertFalse(iter.hasNext());

        TIME_MAP = getUrnToTime(ctCache);
        assertEquals(1, TIME_MAP.size());
        ctCache = null;
        fileManager.clearExcludeURN();
        // ---------------------------
    }


    public void testCommit() throws Exception {
        CreationTimeCache ctCache = null;
        Iterator iter = null;
        Map TIME_MAP = null;
        Long old = new Long(1);
        Long middle = new Long(2);
        Long young = new Long(3);
        deleteCacheFile();
        
        // should be a empty cache
        // ---------------------------
        ctCache = new CreationTimeCache(fileManager);
        assertFalse(ctCache.getFiles().iterator().hasNext());

        TIME_MAP = getUrnToTime(ctCache);
        assertEquals(0, TIME_MAP.size());
        // ---------------------------

        // make sure that only after something is committed is it returned
        // via getFiles().iterator()
        // ---------------------------
        ctCache.addTime(hash1, middle.longValue());
        iter = ctCache.getFiles().iterator();
        assertFalse(iter.hasNext());

        ctCache.commitTime(hash1);
        iter = ctCache.getFiles().iterator();
        assertEquals(hash1, iter.next());
        assertFalse(iter.hasNext());
        // ---------------------------

        // make sure that commiting changes ordering....
        // ---------------------------
        ctCache.addTime(hash2, young.longValue());
        ctCache.addTime(hash3, old.longValue());

        ctCache.commitTime(hash3);
        iter = ctCache.getFiles().iterator();
        assertEquals(hash1, iter.next());
        assertEquals(hash3, iter.next());
        assertFalse(iter.hasNext());

        ctCache.commitTime(hash2);
        iter = ctCache.getFiles().iterator();
        assertEquals(hash2, iter.next());
        assertEquals(hash1, iter.next());
        assertEquals(hash3, iter.next());
        assertFalse(iter.hasNext());
        // ---------------------------
    }



    /**
     * Test read & write of map
     */
    public void testPersistence() throws Exception {
        deleteCacheFile();
        assertTrue("cache should not be present", !cacheExists() );
        
        CreationTimeCache cache = new CreationTimeCache(fileManager);
        FileDesc[] descs = createFileDescs(cache);
        assertNotNull("should have some file descs", descs);
        assertGreaterThan("should have some file descs", 0, descs.length);
        cache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for( int i = 0; i < descs.length; i++) {
            Long cTime = cache.getCreationTime(descs[i].getSHA1Urn());
            assertNotNull("file should be present in cache", cTime);
        }
    }

	private FileDesc[] createFileDescs(CreationTimeCache cache) throws Exception {
        File path = TestUtils.getResourceFile(FILE_PATH);
        File[] files = path.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
		FileDesc[] fileDescs = new FileDesc[files.length];
		for(int i=0; i<files.length; i++) {
			Set<URN> urns = UrnHelper.calculateAndCacheURN(files[i], injector.getInstance(UrnCache.class));            
			fileDescs[i] = new FileDesc(files[i], urns, i);
			cache.addTime(fileDescs[i].getSHA1Urn(),
                                                 files[i].lastModified());
		}				
		return fileDescs;
	}

	private void deleteCacheFile() {
		File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
		cacheFile.delete();
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private boolean cacheExists() {
		File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
		return cacheFile.exists();
	}



	@Singleton
    private static class MyFileManager extends MetaFileManager {
        private FileDesc fd = null;
        private URN toExclude = null;
        private URN defaultURN;
        private Set<URN> validUrns;
        
        @Inject
        public MyFileManager(FileManagerController fileManagerController) {
            super(fileManagerController);
        }
        
        public void setDefaultUrn(URN urn) {
            this.defaultURN = urn;
        }
        
        public void setValidUrns(Set<URN> validUrns) {
            this.validUrns = validUrns;
        }

        public void setExcludeURN(URN urn) {
            toExclude = urn;
        }
        public void clearExcludeURN() {
            toExclude = null;
        }

        @Override
        public FileDesc getFileDescForUrn(URN urn) {
            if (fd == null) {
                Set<URN> urnSet = new HashSet<URN>();
                urnSet.add(defaultURN);
                fd = new FileDesc(new File(_settingsDir, CREATION_CACHE_FILE), urnSet, 0);
            }
            if ((toExclude != null) && toExclude.equals(urn)) {
                return null;
            } else if (validUrns.contains(urn)) {
                return fd;
            } else {
                return super.getFileDescForUrn(urn);
            }
        }
    }

}
