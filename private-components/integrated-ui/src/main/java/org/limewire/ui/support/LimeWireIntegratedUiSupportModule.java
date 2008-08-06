package org.limewire.ui.support;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;


public class LimeWireIntegratedUiSupportModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SessionInfo.class).to(LimeSessionInfo.class);
        bind(LocalClientInfoFactory.class).toProvider(FactoryProvider.newFactory(LocalClientInfoFactory.class, LocalClientInfo.class));
        requestStaticInjection(FatalBugManager.class);
        requestStaticInjection(DeadlockBugManager.class);
        requestStaticInjection(ServletAccessor.class);
        requestStaticInjection(BugManager.class);
    }

}
