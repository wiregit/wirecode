package com.limegroup.gnutella.settings;

import org.limewire.inject.AbstractModule;

public class LimeWireSettingsModule extends AbstractModule {
    protected void configure() {
        bind(XMPPSettings.class);
    }
}
