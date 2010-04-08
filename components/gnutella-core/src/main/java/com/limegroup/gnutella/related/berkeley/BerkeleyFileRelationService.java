package com.limegroup.gnutella.related.berkeley;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Asynchronous;
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
import com.sleepycat.persist.StoreConfig;

@EagerSingleton
public class BerkeleyFileRelationService implements Service {

    private static final Log LOG =
        LogFactory.getLog(BerkeleyFileRelationService.class);

    private final BerkeleyFileRelationCache fileRelationCache;
    private final BerkeleyCache<URN> goodFileCache, badFileCache, playCountCache;
    private final BerkeleyCache<String> extensionCountCache;
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Environment environment;
    private EntityStore fileRelationStore, goodFileStore, badFileStore;
    private EntityStore playCountStore, extensionCountStore;

    @Inject
    BerkeleyFileRelationService(BerkeleyFileRelationCache fileRelationCache,
            @GoodFileCache BerkeleyCache<URN> goodFileCache,
            @BadFileCache BerkeleyCache<URN> badFileCache,
            @PlayCountCache BerkeleyCache<URN> playCountCache,
            @ExtensionCountCache BerkeleyCache<String> extensionCountCache) {
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
            // Transactional StoreConfig for related, plays, extensions
            StoreConfig txnStoreConfig = new StoreConfig();
            txnStoreConfig.setReadOnly(false);
            txnStoreConfig.setAllowCreate(true);
            txnStoreConfig.setTransactional(true);
            // Non-transactional StoreConfig for good, bad
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setReadOnly(false);
            storeConfig.setAllowCreate(true);
            storeConfig.setTransactional(false);
            getFile().mkdirs();
            environment = new Environment(getFile(), environmentConfig);
            fileRelationStore =
                new EntityStore(environment, "related", txnStoreConfig);
            fileRelationCache.setIndex(fileRelationStore.getPrimaryIndex(
                    String.class, UrnSet.class));
            fileRelationCache.setEnvironment(environment);
            goodFileStore = new EntityStore(environment, "good", storeConfig);
            goodFileCache.setIndex(goodFileStore.getPrimaryIndex(
                    String.class, Count.class));
            goodFileCache.setEnvironment(environment);
            badFileStore = new EntityStore(environment, "bad", storeConfig);
            badFileCache.setIndex(badFileStore.getPrimaryIndex(
                    String.class, Count.class));
            badFileCache.setEnvironment(environment);
            playCountStore =
                new EntityStore(environment, "plays", txnStoreConfig);
            playCountCache.setIndex(playCountStore.getPrimaryIndex(
                    String.class, Count.class));
            playCountCache.setEnvironment(environment);
            extensionCountStore =
                new EntityStore(environment, "extensions", txnStoreConfig);
            extensionCountCache.setIndex(extensionCountStore.getPrimaryIndex(
                    String.class, Count.class));
            extensionCountCache.setEnvironment(environment);
            LOG.debug("Opened database");
        } catch(DatabaseException e) {
            LOG.error("Error opening database", e);
        }
    }

    @Override
    public void stop() {
        if(closed.getAndSet(true))
            return;
        LOG.debug("Closing database");
        try {
            if(fileRelationStore != null)
                fileRelationStore.close();
            if(goodFileStore != null)
                goodFileStore.close();
            if(badFileStore != null)
                badFileStore.close();
            if(playCountStore != null)
                playCountStore.close();
            if(extensionCountStore != null)
                extensionCountStore.close();
            if(environment != null)
                environment.close();
            LOG.debug("Closed database");
        } catch(DatabaseException e) {
            LOG.error("Error closing database", e);
        }
    }

    Environment getEnvironment() {
        return environment;
    }

    private File getFile() {
        return new File(CommonUtils.getUserSettingsDir(), "related");
    }
}
