package com.limegroup.gnutella.related;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.security.SHA1;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.URN;

@EagerSingleton
class StressTest implements Service {

    private static final int THREADS = 10;
    private static final long DURATION = 15 * 60 * 1000;

    private static final Log LOG = LogFactory.getLog(StressTest.class);

    private final FileRelationManager fileRelationManager;
    private final FileRelationCache fileRelationCache;
    private final CountDownLatch latch;

    @Inject
    StressTest(FileRelationManager fileRelationManager,
            FileRelationCache fileRelationCache) {
        this.fileRelationManager = fileRelationManager;
        this.fileRelationCache = fileRelationCache;
        latch = new CountDownLatch(THREADS);
    }

    @Inject
    public void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public String getServiceName() {
        return "Related Files Stress Test";
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        new ManagedThread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(60 * 1000);
                } catch(InterruptedException ignored) {}
                if(LOG.isInfoEnabled()) {
                    LOG.info(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                    LOG.info(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
                }
                goCrazy();
                try {
                    latch.await();
                } catch(InterruptedException ignored) {}
                if(LOG.isInfoEnabled()) {
                    LOG.info(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                    LOG.info(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
                }
            }
        }.start();
    }

    @Override
    public void stop() {
    }

    // Runs all the tests at once in several threads
    void goCrazy() {
        for(int i = 0; i < THREADS; i++) {
            new ManagedThread() {
                @Override
                public void run() {
                    LOG.info("Thread started");
                    long start = System.currentTimeMillis();
                    while(System.currentTimeMillis() - start < DURATION) {
                        chooseAndRunTest();
                        try {
                            Thread.sleep((long)(Math.random() * 2000));
                        } catch(InterruptedException ignored) {}
                    }
                    LOG.info("Thread finished");
                    latch.countDown();
                }
            }.start();
        }
    }

    // Randomly chooses a test and runs it
    void chooseAndRunTest() {
        double roulette = Math.random();
        if(roulette < 0.25)
            browseTest(); // p = 0.25
        else if(roulette < 0.5)
            searchResultTest(); // p = 0.25
        else if(roulette < 0.75)
            downloadTest(); // p = 0.25
        else if(roulette < 0.95)
            markAsGoodTest(); // p = 0.2
        else
            markAsBadTest(); // p = 0.05, to allow the cache size to grow
    }

    // Creates a browse of 100-1000 files
    void browseTest() {
        SHA1 digest = new SHA1();
        int size = 10 * ((int)(Math.random() * 100) + 1);
        if(LOG.isDebugEnabled())
            LOG.debug("Creating browse with " + size + " files");
        TreeSet<URN> browse = new TreeSet<URN>();
        for(int j = 0; j < size; j++) {
            try {
                browse.add(createRandomUrn(digest));
            } catch(IOException e) {
                LOG.error("Error creating URN", e);
            }
        }
        fileRelationCache.addRelations(browse);
        LOG.debug("Browse finished");
    }

    // Looks up a batch of 100-1000 search results
    void searchResultTest() {
        SHA1 digest = new SHA1();
        int size = 10 * ((int)(Math.random() * 100) + 1);
        if(LOG.isDebugEnabled())
            LOG.debug("Creating search with " + size + " results");
        for(int j = 0; j < size; j++) {
            try {
                URN u = createRandomUrn(digest);
                String filename = createRandomFilename();
                fileRelationManager.getNumberOfRelatedGoodFiles(u);
                fileRelationManager.guessDownloadProbability(filename);
            } catch(IOException e) {
                LOG.error("Error creating URN", e);
            }
        }
        LOG.debug("Search finished");
    }

    // Marks a file as good
    void markAsGoodTest() {
        try {
            LOG.debug("Marking a file as good");
            fileRelationManager.markFileAsGood(createRandomUrn(new SHA1()));
        } catch(IOException e) {
            LOG.error("Error creating URN", e);
        }
    }

    // Marks a file as bad
    void markAsBadTest() {
        try {
            LOG.debug("Marking a file as bad");
            fileRelationManager.markFileAsBad(createRandomUrn(new SHA1()));
        } catch(IOException e) {
            LOG.error("Error creating URN", e);
        }
    }

    // Starts a download
    void downloadTest() {
        LOG.debug("Starting a download");
        fileRelationManager.downloadStarted(createRandomFilename());
    }

    // Returns a random URN from a restricted space of 100,000 possibilities
    private URN createRandomUrn(SHA1 digest) throws IOException {
        int a = (int)(Math.random() * 100);
        int b = (int)(Math.random() * 1000);
        byte[] hash = new byte[8];
        ByteUtils.int2beb(a, hash, 0);
        ByteUtils.int2beb(b, hash, 4);
        digest.reset();
        digest.update(hash);
        return URN.createSHA1UrnFromBytes(digest.digest());
    }

    // Returns a filename with one of 20 possible extensions
    private String createRandomFilename() {
        return "abc." + (int)(Math.random() * 20);
    }
}