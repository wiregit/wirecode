package org.limewire.core.impl.xmpp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class XMPPConfigurationListProvider extends ArrayList<XMPPConnectionConfiguration> {

    @Inject
    XMPPConfigurationListProvider(Provider<Map<String, XMPPServerSettings.XMPPServerConfiguration>> serverConfigs,
                                  Provider<Map<String, XMPPUserSettings.XMPPUserConfiguration>> userConfigs/*,
                                  RosterListener rosterListener,
                                  XMPPErrorListener errorListener*/) {
        for(String serviceName : serverConfigs.get().keySet()) {
            XMPPServerSettings.XMPPServerConfiguration serverConfiguration = serverConfigs.get().get(serviceName);
            XMPPUserSettings.XMPPUserConfiguration userConfiguration = userConfigs.get().get(serviceName);
            if(userConfiguration == null) {
                userConfiguration = new XMPPUserSettings.XMPPUserConfiguration(serviceName);
            }
            // TODO per-configuration RosterListeners, XMPPErrorListeners
            add(new XMPPConfigurationImpl(serverConfiguration, userConfiguration, null, null));
        }
    }
}
