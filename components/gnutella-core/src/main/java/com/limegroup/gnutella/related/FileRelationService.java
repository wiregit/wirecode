package com.limegroup.gnutella.related;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Join;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.related.annotations.BadFileCache;
import com.limegroup.gnutella.related.annotations.ExtensionCountCache;
import com.limegroup.gnutella.related.annotations.GoodFileCache;
import com.limegroup.gnutella.related.annotations.PlayCountCache;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;

@EagerSingleton
class FileRelationService implements Service {

    private static final Log LOG = LogFactory.getLog(FileRelationService.class);

    private final BerkeleyFileRelationCache fileRelationCache;
    private final BerkeleyFileCache goodFileCache, badFileCache;
    private final InMemoryCountCache<URN> playCountCache;
    private final InMemoryCountCache<String> extensionCountCache;
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Environment environment;
    private EntityStore relatedStore, goodStore, badStore;
    private EntityStore playCountStore, extCountStore;

    @Inject
    FileRelationService(BerkeleyFileRelationCache fileRelationCache,
            @GoodFileCache BerkeleyFileCache goodFileCache,
            @BadFileCache BerkeleyFileCache badFileCache,
            @PlayCountCache InMemoryCountCache<URN> playCountCache,
            @ExtensionCountCache InMemoryCountCache<String> extensionCountCache) {
        this.fileRelationCache = fileRelationCache;
        this.goodFileCache = goodFileCache;
        this.badFileCache = badFileCache;
        this.playCountCache = playCountCache;
        this.extensionCountCache = extensionCountCache;
    }

    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public String getServiceName() {
        return "FileRelationService";
    }

    @Override
    public void initialize() {
    }

    @Asynchronous
    @Override
    public void start() {
        if(open.getAndSet(true))
            return;
        LOG.debug("Opening database");
        try {
            EnvironmentConfig environmentConfig = new EnvironmentConfig();
            environmentConfig.setReadOnly(false);
            environmentConfig.setAllowCreate(true);
            environmentConfig.setTransactional(true);
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setReadOnly(false);
            storeConfig.setAllowCreate(true);
            storeConfig.setTransactional(true);
            getFile().mkdirs();
            environment = new Environment(getFile(), environmentConfig);

            relatedStore = new EntityStore(environment, "related", storeConfig);
            PrimaryIndex<String, UrnSet> relatedPrimary =
                relatedStore.getPrimaryIndex(String.class, UrnSet.class);
            SecondaryIndex<Long, String, UrnSet> relatedSecondary =
                relatedStore.getSecondaryIndex(relatedPrimary, long.class, "accessTime");
            fileRelationCache.setIndex(relatedPrimary);
            fileRelationCache.setAccessTimeIndex(relatedSecondary);
            fileRelationCache.setEnvironment(environment);

            goodStore = new EntityStore(environment, "good", storeConfig);
            PrimaryIndex<String, Urn> goodPrimary =
                goodStore.getPrimaryIndex(String.class, Urn.class);
            SecondaryIndex<Long, String, Urn> goodSecondary =
                goodStore.getSecondaryIndex(goodPrimary, long.class, "accessTime");
            goodFileCache.setIndex(goodPrimary);
            goodFileCache.setAccessTimeIndex(goodSecondary);
            goodFileCache.setEnvironment(environment);

            badStore = new EntityStore(environment, "bad", storeConfig);
            PrimaryIndex<String, Urn> badPrimary =
                badStore.getPrimaryIndex(String.class, Urn.class);
            SecondaryIndex<Long, String, Urn> badSecondary =
                badStore.getSecondaryIndex(badPrimary, long.class, "accessTime");
            badFileCache.setIndex(badPrimary);
            badFileCache.setAccessTimeIndex(badSecondary);
            badFileCache.setEnvironment(environment);

            LOG.debug("Opened database");
        } catch(DatabaseException e) {
            LOG.error("Error opening database", e);
        }
        playCountCache.load();
        extensionCountCache.load();
    }

    @Asynchronous(join=Join.TIMEOUT, timeout=60, daemon=false)
    @Override
    public void stop() {
        if(closed.getAndSet(true))
            return;
        LOG.debug("Closing database");
        try {
            if(relatedStore != null)
                relatedStore.close();
            if(goodStore != null)
                goodStore.close();
            if(badStore != null)
                badStore.close();
            if(playCountStore != null)
                playCountStore.close();
            if(extCountStore != null)
                extCountStore.close();
            if(environment != null)
                environment.close();
            LOG.debug("Closed database");
        } catch(DatabaseException e) {
            LOG.error("Error closing database", e);
        }
        playCountCache.save();
        extensionCountCache.save();
    }

    Environment getEnvironment() {
        return environment;
    }

    private File getFile() {
        return new File(CommonUtils.getUserSettingsDir(), "related");
    }
}
