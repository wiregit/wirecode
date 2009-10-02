package org.limewire.core.impl.file;

import org.limewire.core.api.file.CategoryManager;

import com.google.inject.AbstractModule;


public class MockFileModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(CategoryManager.class).to(MockCategoryManager.class);
    }

}
