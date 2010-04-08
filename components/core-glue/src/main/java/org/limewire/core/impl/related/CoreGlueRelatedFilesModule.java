package org.limewire.core.impl.related;

import org.limewire.core.api.related.RelatedFiles;

import com.google.inject.AbstractModule;

public class CoreGlueRelatedFilesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RelatedFiles.class).to(RelatedFilesImpl.class);
    }
}
