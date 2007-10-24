package org.limewire.net;

import org.limewire.util.OSUtils;

import com.google.inject.AbstractModule;

/**
 * A Guice-module for binding all net related activity.
 * This class can be constructed with or without a ProxySettings.
 * If it is without, then a default no-proxy settings is used.
 */
public class LimeWireNetModule extends AbstractModule {
    
    private final Class<? extends ProxySettings> proxySettings;
    private final Class<? extends SocketBindingSettings> socketBindingSettings;
    
    /** Constructs the module without using any proxies. */
    public LimeWireNetModule() {
        this(EmptyProxySettings.class, EmptySocketBindingSettings.class);
    }
    
    /** Constructs the module with the given settings for using proxies. */
    public LimeWireNetModule(Class<? extends ProxySettings> proxySettings, Class<? extends SocketBindingSettings> socketBindingSettings) {
        this.proxySettings = proxySettings;
        this.socketBindingSettings = socketBindingSettings;
    }

    @Override
    protected void configure() {
        bind(SocketsManager.class).to(SocketsManagerImpl.class);
        bind(ProxyManager.class).to(ProxyManagerImpl.class);
        
        if(OSUtils.isSocketChallengedWindows())
            bind(SocketController.class).to(LimitedSocketController.class);
        else
            bind(SocketController.class).to(SimpleSocketController.class);
        
        bind(ProxySettings.class).to(proxySettings);
        bind(SocketBindingSettings.class).to(socketBindingSettings);
    }

}
