package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.helpers.UrnHelper;


/**
 * Tests the public methods of the UrnCache class.
 */
@SuppressWarnings("unchecked")
public final class UrnCacheTest extends LimeTestCase {

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String URN_CACHE_FILE = "fileurns.cache";
    private static final String FILE_PATH = "com/limegroup/gnutella/util";
    private static final String AUDIO_PATH = "com/limegroup/gnutella/resources/";

    private static final Set EMPTY_SET = 
        Collections.unmodifiableSet(new HashSet());
    private UrnCache urnCache;
    
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
	
	@Override
	protected void setUp() throws Exception {
	    urnCache = new UrnCache(ExecutorsHelper.newProcessingQueue("test"), null);
	}
    
    /**
     * Test read & write of map
     */
    public void testPersistence() throws Exception {
        assertTrue("cache should not be present", !cacheExists() );
        
        List<FileAndUrns> faus = getLotsOfFiles();
        calculateSHA1s(faus);

        assertNotNull("should have some file descs", faus);
        assertGreaterThan("should have some file descs", 0, faus.size());
        assertTrue("cache should still not be present", !cacheExists() );
        urnCache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for(FileAndUrns fau : faus) {
            Set set = urnCache.getUrns(fau.file);
            assertTrue("file should be present in cache", !set.equals(EMPTY_SET));
            assertTrue("URN set should not be empty", !set.isEmpty());
            assertEquals("URN set should be same as that in desc", fau.urns, set);
        }
    }
    
    public void testPersistsItself() throws Exception {
        Mockery context = new Mockery();
        final ServiceScheduler serviceScheduler = context.mock(ServiceScheduler.class);
        final AtomicReference<Runnable> runnable = new AtomicReference<Runnable>();
        context.checking(new Expectations() {{
            one(serviceScheduler).scheduleWithFixedDelay(with(any(String.class)), 
                    with(any(Runnable.class)), with(equal(30L)), with(equal(30L)), 
                    with(equal(TimeUnit.SECONDS)), with(aNull(ScheduledExecutorService.class)));
            will(new AssignParameterAction<Runnable>(runnable, 1));
        }});
        
        urnCache.register(null, serviceScheduler);
        
        List<FileAndUrns> faus = getLotsOfFiles();
        calculateSHA1s(faus);

        assertNotNull("should have some file descs", faus);
        assertGreaterThan("should have some file descs", 0, faus.size());
        assertTrue("cache should still not be present", !cacheExists() );
                
        runnable.get().run();
        
        assertTrue("runnable should have written cache", cacheExists());
        
        context.assertIsSatisfied();
    }
    
    public void testNMS1Cached() throws Exception {
        assertTrue("cache should not be present", !cacheExists() );
        
        List<FileAndUrns> faus = getLotsOfAudioFiles();
        calculateSHA1s(faus);
        calculateNMS1s(faus);

        assertNotNull("should have some file descs", faus);
        assertGreaterThan("should have some file descs", 0, faus.size());
        assertTrue("cache should still not be present", !cacheExists() );
        urnCache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for(FileAndUrns fau : faus) {
            Set set = urnCache.getUrns(fau.file);
            assertNotNull(UrnSet.getSha1(set));
            assertNotNull(UrnSet.getNMS1(set));
            assertEquals("URN set should be same as that in desc", fau.urns, set);
        }
    }
    
    public void testSHA1CreatedAndCachedWithExistingNMS1() throws Exception {
        assertTrue("cache should not be present", !cacheExists() );
        
        // create Non-Metadata SHA1s
        List<FileAndUrns> faus = getLotsOfAudioFiles();
        calculateNMS1s(faus);

        assertNotNull("should have some file descs", faus);
        assertGreaterThan("should have some file descs", 0, faus.size());
        assertTrue("cache should still not be present", !cacheExists() );
        urnCache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        // assert NMS1 exists and SHA1 does not
        for(FileAndUrns fau : faus) {
            Set set = urnCache.getUrns(fau.file);
            assertNull(UrnSet.getSha1(set));
            assertNotNull(UrnSet.getNMS1(set));
            assertEquals("URN set should be same as that in desc", fau.urns, set);
        }
        
        // calculate SHA1s
        calculateSHA1s(faus);
        urnCache.persistCache();
        // assert NMS1 and SHA1 exists
        for(FileAndUrns fau : faus) {
            Set set = urnCache.getUrns(fau.file);
            assertNotNull(UrnSet.getSha1(set));
            assertNotNull(UrnSet.getNMS1(set));
            assertEquals("URN set should be same as that in desc", fau.urns, set);
        }
    }

    /**
     * Calculates SHA1s for all of the files within the list.
     */
    private void calculateSHA1s(List<FileAndUrns> faus) throws Exception {
	    for(FileAndUrns files : faus) {
	        Set<URN> urns = UrnHelper.calculateAndCacheURN(files.file, urnCache);
	        files.addAll(urns);
	    }
	}
	
    /**
     * Calculates NMS1s for all the files within the list.
     */
	private void calculateNMS1s(List<FileAndUrns> faus) throws Exception {
	    for(FileAndUrns files : faus) {
	        URN nms1 = urnCache.calculateAndCacheNMS1(files.file).get();
            if(nms1 != null)
                files.addURN(nms1);
	    }
	}
	
	/**
	 * Returns a List of FileAndUrns, filled with a bunch of 
	 * files from the given path. Urns must be loaded separately.
	 */
	private List<FileAndUrns> getLotsOfFiles() {
	    List<FileAndUrns> faus = new ArrayList<FileAndUrns>();
        File path = TestUtils.getResourceFile(FILE_PATH);
        File[] files = path.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        
        for(File file : files) {
            faus.add(new FileAndUrns(file));
        }
        return faus;
	}
	
	/**
     * Returns a List of FileAndUrns, filled with a bunch of audio
     * files from the given path. Urns must be loaded separately.
     */
	private List<FileAndUrns> getLotsOfAudioFiles() {
	    List<FileAndUrns> faus = new ArrayList<FileAndUrns>();
        File path = TestUtils.getResourceFile(AUDIO_PATH);
        File[] files = path.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                String ext = FileUtils.getFileExtension(file);
                // only return valid mp3s in the resource directory
                return !file.isDirectory() && ext.equalsIgnoreCase("mp3") && file.length() > 1000;
            }
        });  
        
        for(File file : files) {
            faus.add(new FileAndUrns(file));
        }
        return faus;
	}	
	
	private class FileAndUrns {
	    private final Set<URN> urns;
	    private final File file;
	    
	    public FileAndUrns(File file) {
            this.urns = new HashSet<URN>();
            this.file = file;
        }
	    
	    public void addURN(URN urn) {
	        urns.add(urn);
	    }
	    
	    public void addAll(Set<URN> urns) {
	        this.urns.addAll(urns);
	    }
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private static boolean cacheExists() {
		File cacheFile = new File(_settingsDir, URN_CACHE_FILE);
		return cacheFile.exists();
	}

}

