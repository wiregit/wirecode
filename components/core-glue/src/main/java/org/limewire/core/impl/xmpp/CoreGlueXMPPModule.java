package org.limewire.core.impl.xmpp;

import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.friend.api.PasswordManager;
import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.client.LimeWireXMPPModule;
import org.limewire.xmpp.client.impl.PasswordManagerImpl;

import com.limegroup.gnutella.settings.SettingsBackedJabberSettings;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueXMPPModule extends AbstractModule {
    @Override
    protected void configure() {
        binder().install(new LimeWireXMPPModule(SettingsBackedJabberSettings.class));
        
        
        bind(PasswordManager.class).to(PasswordManagerImpl.class);
        bind(XMPPResourceFactory.class).to(XMPPResourceFactoryImpl.class);
        
        bind(IdleTime.class).to(IdleTimeImpl.class);
        
        bind(IdleStatusMonitorFactory.class).toProvider(
                FactoryProvider.newFactory(
                        IdleStatusMonitorFactory.class, IdleStatusMonitor.class));
    }
}
