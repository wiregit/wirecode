package com.limegroup.gnutella.related;

import org.limewire.inject.AbstractModule;

import com.google.inject.TypeLiteral;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.related.annotations.BadFileCache;
import com.limegroup.gnutella.related.annotations.ExtensionCountCache;
import com.limegroup.gnutella.related.annotations.GoodFileCache;
import com.limegroup.gnutella.related.annotations.PlayCountCache;
import com.limegroup.gnutella.related.berkeley.BerkeleyCache;
import com.limegroup.gnutella.related.berkeley.BerkeleyFileRelationService;
import com.limegroup.gnutella.related.berkeley.BerkeleyFileRelationCache;
import com.limegroup.gnutella.related.memory.InMemoryCache;
import com.limegroup.gnutella.related.memory.InMemoryFileRelationCache;
import com.limegroup.gnutella.related.memory.InMemoryFileRelationService;

public class LimeWireRelatedFilesModule extends AbstractModule {

    boolean inMemory = false, stressTest = false;

    @Override
    protected void configure() {
        bind(FileRelationManager.class).to(FileRelationManagerImpl.class);
        if(inMemory) {
            InMemoryFileRelationCache fileRelationCache =
                new InMemoryFileRelationCache("related.dat", 10, 50000);
            InMemoryCache<URN> goodFileCache =
                new InMemoryCache<URN>("good.dat", 10, 5000);
            InMemoryCache<URN> badFileCache =
                new InMemoryCache<URN>("bad.dat", 10, 500);
            InMemoryCache<URN> playCountCache =
                new InMemoryCache<URN>("plays.dat", 10, 500);
            InMemoryCache<String> extensionCountCache =
                new InMemoryCache<String>("extensions.dat", 10, 50);

            bind(FileRelationCache.class).toInstance(fileRelationCache);
            bind(InMemoryFileRelationCache.class).toInstance(fileRelationCache);
            bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                    GoodFileCache.class).toInstance(goodFileCache);
            bind(new TypeLiteral<InMemoryCache<URN>>(){}).annotatedWith(
                    GoodFileCache.class).toInstance(goodFileCache);
            bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                    BadFileCache.class).toInstance(badFileCache);
            bind(new TypeLiteral<InMemoryCache<URN>>(){}).annotatedWith(
                    BadFileCache.class).toInstance(badFileCache);
            bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                    PlayCountCache.class).toInstance(playCountCache);
            bind(new TypeLiteral<InMemoryCache<URN>>(){}).annotatedWith(
                    PlayCountCache.class).toInstance(playCountCache);
            bind(new TypeLiteral<Cache<String>>(){}).annotatedWith(
                    ExtensionCountCache.class).toInstance(extensionCountCache);
            bind(new TypeLiteral<InMemoryCache<String>>(){}).annotatedWith(
                    ExtensionCountCache.class).toInstance(extensionCountCache);

            bind(InMemoryFileRelationService.class);
        } else {
            BerkeleyFileRelationCache fileRelationCache =
                new BerkeleyFileRelationCache();
            BerkeleyCache<URN> goodFileCache =
                new BerkeleyCache<URN>("good");
            BerkeleyCache<URN> badFileCache =
                new BerkeleyCache<URN>("bad");
            BerkeleyCache<URN> playCountCache =
                new BerkeleyCache<URN>("plays");
            BerkeleyCache<String> extensionCountCache =
                new BerkeleyCache<String>("extensions");

            bind(FileRelationCache.class).toInstance(fileRelationCache);
            bind(BerkeleyFileRelationCache.class).toInstance(fileRelationCache);
            bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                    GoodFileCache.class).toInstance(goodFileCache);
            bind(new TypeLiteral<BerkeleyCache<URN>>(){}).annotatedWith(
                    GoodFileCache.class).toInstance(goodFileCache);
            bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                    BadFileCache.class).toInstance(badFileCache);
            bind(new TypeLiteral<BerkeleyCache<URN>>(){}).annotatedWith(
                    BadFileCache.class).toInstance(badFileCache);
            bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                    PlayCountCache.class).toInstance(playCountCache);
            bind(new TypeLiteral<BerkeleyCache<URN>>(){}).annotatedWith(
                    PlayCountCache.class).toInstance(playCountCache);
            bind(new TypeLiteral<Cache<String>>(){}).annotatedWith(
                    ExtensionCountCache.class).toInstance(extensionCountCache);
            bind(new TypeLiteral<BerkeleyCache<String>>(){}).annotatedWith(
                    ExtensionCountCache.class).toInstance(extensionCountCache);

            bind(BerkeleyFileRelationService.class);            
        }
        if(stressTest) {
            bind(StressTest.class);
        }
    }
}
