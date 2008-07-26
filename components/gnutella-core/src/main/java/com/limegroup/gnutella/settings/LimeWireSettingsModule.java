package com.limegroup.gnutella.settings;

import org.limewire.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import java.util.List;

public class LimeWireSettingsModule extends AbstractModule {
    protected void configure() {
        //bind(XMPPSettingsImpl.class);
        bind(new TypeLiteral<List<XMPPSettings.XMPPServerConfiguration>>(){}).toProvider(XMPPSettings.XMPPServerConfigs.class);
    }
}
