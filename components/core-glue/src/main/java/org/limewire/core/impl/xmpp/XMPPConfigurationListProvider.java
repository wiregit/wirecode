package org.limewire.core.impl.xmpp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPErrorListener;

import java.util.ArrayList;
import java.util.List;

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
