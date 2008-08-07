package org.limewire.xmpp.client;

import java.util.List;
import java.util.Arrays;

import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

import com.google.inject.Provider;

public class XMPPConnectionConfigurationListProvider implements Provider<List<XMPPConnectionConfiguration>> {
    private final XMPPConnectionConfiguration[] configurations;

    public XMPPConnectionConfigurationListProvider(XMPPConnectionConfiguration ... configurations) {
        this.configurations = configurations;
    }
    
    public List<XMPPConnectionConfiguration> get() {
        return Arrays.asList(configurations);
    }
}
