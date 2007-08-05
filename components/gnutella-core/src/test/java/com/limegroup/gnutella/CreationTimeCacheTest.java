package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.xml.MetaFileManager;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
@SuppressWarnings("unchecked")
public class CreationTimeCacheTest 
    extends com.limegroup.gnutella.util.LimeTestCase {
    private static final int PORT=6669;

    private static RouterService rs;

    private static MyActivityCallback callback;
    private static MyFileManager fm;

    private static URN hash1;
    private static URN hash2;
    private static URN hash3;
    private static URN hash4;

    static {
        try {
        hash1 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSASUSH");
        hash2 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSANITA");
        hash3 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQABOALT");
        hash4 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5BERKELEY");
        }
        catch (Exception terrible) {}
    }

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String CREATION_CACHE_FILE = "createtimes.cache";
    private static final String FILE_PATH = "com/limegroup/gnutella/util";


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

    private static void doSettings() {
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;mp3");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        File mp3 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/metadata/mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3");
        // now move them to the share dir
        FileUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        FileUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        FileUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        callback=new MyActivityCallback();
        fm = new MyFileManager();
        rs= new RouterService(callback, ProviderHacks.getMessageRouter());

        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        RouterService.clearHostCatcher();
        RouterService.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());

    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }

    ///////////////////////// Actual Tests ////////////////////////////

    private CreationTimeCache getCTC() throws Exception {
        return new CreationTimeCache(fm);
    }
    
    private Map getUrnToTime(CreationTimeCache cache) throws Exception {
        Future<?> future = (Future)PrivilegedAccessor.getValue(cache, "deserializer");
        Object o = future.get();
        return (Map)PrivilegedAccessor.invokeMethod(o, "getUrnToTime");
    }

    /** Tests that the URN_MAP is derived correctly from the URN_TO_TIME_MAP
     */
    public void testMapCreation() throws Exception {
        // mock up our own createtimes.txt
        Map toSerialize = new HashMap();
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
        CreationTimeCache ctCache = getCTC();
        Map map = getUrnToTime(ctCache);
        assertEquals(toSerialize, map);
    }


    /** Tests the getFiles().iterator() method.
     */
    public void testGetFiles() throws Exception {
        // mock up our own createtimes.txt
        Map toSerialize = new HashMap();
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
        CreationTimeCache ctCache = getCTC();
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
        CreationTimeCache ctCache = null;
        Iterator iter = null;
        Map TIME_MAP = null;
        Long old = new Long(1);
        Long middle = new Long(2);
        Long young = new Long(3);
        deleteCacheFile();
        
        // should be a empty cache
        // ---------------------------
        ctCache = getCTC();
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
        ctCache = getCTC();
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
        ctCache = getCTC();
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
        fm.setExcludeURN(hash4);
        ctCache = getCTC();
        iter = ctCache.getFiles().iterator();
        assertEquals(hash2, iter.next());
        assertFalse(iter.hasNext());

        TIME_MAP = getUrnToTime(ctCache);
        assertEquals(1, TIME_MAP.size());
        ctCache = null;
        fm.clearExcludeURN();
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
        ctCache = getCTC();
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
        
        CreationTimeCache cache = ProviderHacks.getCreationTimeCache();
        FileDesc[] descs = createFileDescs();
        assertNotNull("should have some file descs", descs);
        assertGreaterThan("should have some file descs", 0, descs.length);
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
			Set urns = calculateAndCacheURN(files[i]);            
			fileDescs[i] = new FileDesc(files[i], urns, i);
            ProviderHacks.getCreationTimeCache().addTime(fileDescs[i].getSHA1Urn(),
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



    //////////////////////////////////////////////////////////////////

    public static class MyActivityCallback extends ActivityCallbackStub {
    }

    public static class MyFileManager extends MetaFileManager {
        private FileDesc fd = null;
        public URN toExclude = null;
        
        public MyFileManager() {
            super(ProviderHacks.getFileManagerController());
        }

        public void setExcludeURN(URN urn) {
            toExclude = urn;
        }
        public void clearExcludeURN() {
            toExclude = null;
        }

        public FileDesc getFileDescForUrn(URN urn){
            if (fd == null) {
                File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
                Set urnSet = new HashSet();
                urnSet.add(hash1);
                fd = new FileDesc(cacheFile, urnSet, 0);
            }
            if ((toExclude != null) && toExclude.equals(urn)) return null;
            else if (urn.equals(hash1) ||
                     urn.equals(hash2) ||
                     urn.equals(hash3) ||
                     urn.equals(hash4)) return fd;
            else return super.getFileDescForUrn(urn);
        }
    }

}
