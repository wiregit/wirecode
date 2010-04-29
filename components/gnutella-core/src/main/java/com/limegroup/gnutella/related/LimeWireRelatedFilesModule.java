package com.limegroup.gnutella.related;

import org.limewire.inject.AbstractModule;

import com.google.inject.TypeLiteral;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.related.annotations.BadFileCache;
import com.limegroup.gnutella.related.annotations.ExtensionCountCache;
import com.limegroup.gnutella.related.annotations.GoodFileCache;
import com.limegroup.gnutella.related.annotations.PlayCountCache;

public class LimeWireRelatedFilesModule extends AbstractModule {

    boolean stressTest = false;

    @Override
    protected void configure() {
        bind(FileRelationManager.class).to(FileRelationManagerImpl.class);

        BerkeleyFileRelationCache fileRelationCache =
            new BerkeleyFileRelationCache();
        BerkeleyFileCache goodFileCache = new BerkeleyFileCache("good");
        BerkeleyFileCache badFileCache = new BerkeleyFileCache("bad");

        InMemoryCountCache<URN> playCountCache =
            new InMemoryCountCache<URN>("plays.dat", 10, 500);
        InMemoryCountCache<String> extensionCountCache =
            new InMemoryCountCache<String>("extensions.dat", 10, 50);

        bind(FileRelationCache.class).toInstance(fileRelationCache);
        bind(BerkeleyFileRelationCache.class).toInstance(fileRelationCache);
        bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                GoodFileCache.class).toInstance(goodFileCache);
        bind(new TypeLiteral<BerkeleyFileCache>(){}).annotatedWith(
                GoodFileCache.class).toInstance(goodFileCache);
        bind(new TypeLiteral<Cache<URN>>(){}).annotatedWith(
                BadFileCache.class).toInstance(badFileCache);
        bind(new TypeLiteral<BerkeleyFileCache>(){}).annotatedWith(
                BadFileCache.class).toInstance(badFileCache);

        bind(new TypeLiteral<CountCache<URN>>(){}).annotatedWith(
                PlayCountCache.class).toInstance(playCountCache);
        bind(new TypeLiteral<InMemoryCountCache<URN>>(){}).annotatedWith(
                PlayCountCache.class).toInstance(playCountCache);
        bind(new TypeLiteral<CountCache<String>>(){}).annotatedWith(
                ExtensionCountCache.class).toInstance(extensionCountCache);
        bind(new TypeLiteral<InMemoryCountCache<String>>(){}).annotatedWith(
                ExtensionCountCache.class).toInstance(extensionCountCache);

        bind(FileRelationService.class);            

        if(stressTest) {
            bind(StressTest.class);
        }
    }
}
