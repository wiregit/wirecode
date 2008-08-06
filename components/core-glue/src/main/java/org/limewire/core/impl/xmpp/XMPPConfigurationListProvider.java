package org.limewire.core.impl.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class XMPPConfigurationListProvider extends ArrayList<XMPPConnectionConfiguration> {

    @Inject
    XMPPConfigurationListProvider(Provider<List<XMPPSettings.XMPPServerConfiguration>> serverConfigs/*,
                                  RosterListener rosterListener,
                                  XMPPErrorListener errorListener*/) {
        for(XMPPSettings.XMPPServerConfiguration serverConfiguration : serverConfigs.get()) {
            // TODO per-configuration RosterListeners, XMPPErrorListeners
            add(new XMPPConfigurationImpl(serverConfiguration, null, null));
        }
    }
}
