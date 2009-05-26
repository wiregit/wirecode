package org.limewire.inject;

public class LimeWireInjectModule extends AbstractModule {

    @Override
    protected void configure() {
        bindScope(LazySingleton.class, MoreScopes.LAZY_SINGLETON);   
    }
}
