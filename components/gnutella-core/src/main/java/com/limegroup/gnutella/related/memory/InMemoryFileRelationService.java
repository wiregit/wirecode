package com.limegroup.gnutella.related.memory;

import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Join;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.related.annotations.BadFileCache;
import com.limegroup.gnutella.related.annotations.ExtensionCountCache;
import com.limegroup.gnutella.related.annotations.GoodFileCache;
import com.limegroup.gnutella.related.annotations.PlayCountCache;

@EagerSingleton
public class InMemoryFileRelationService implements Service {

    private final InMemoryFileRelationCache fileRelationCache;
    private final InMemoryCache<URN> goodFileCache, badFileCache;
    private final InMemoryCache<URN> playCountCache;
    private final InMemoryCache<String> extensionCountCache;
    private volatile boolean loading = false;

    @Inject
    InMemoryFileRelationService(InMemoryFileRelationCache fileRelationCache,
            @GoodFileCache InMemoryCache<URN> goodFileCache,
            @BadFileCache InMemoryCache<URN> badFileCache,
            @PlayCountCache InMemoryCache<URN> playCountCache,
            @ExtensionCountCache InMemoryCache<String> extensionCountCache) {
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
        loading = true;
        fileRelationCache.load();
        goodFileCache.load();
        badFileCache.load();
        playCountCache.load();
        extensionCountCache.load();
        loading = false;
    }

    @Asynchronous(join=Join.TIMEOUT, timeout=60, daemon=false)
    @Override
    public void stop() {
        if(loading)
            return;
        fileRelationCache.save();
        goodFileCache.save();
        badFileCache.save();
        playCountCache.save();
        extensionCountCache.save();
    }
}
