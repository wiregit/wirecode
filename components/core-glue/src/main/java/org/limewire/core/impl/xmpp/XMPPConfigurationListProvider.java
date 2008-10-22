package org.limewire.core.impl.xmpp;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.limewire.security.SHA1;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;

@Singleton
class XMPPConfigurationListProvider extends ArrayList<XMPPConnectionConfiguration> {

    @Inject
    XMPPConfigurationListProvider(Provider<Map<String, XMPPServerSettings.XMPPServerConfiguration>> serverConfigs,
                                  Provider<Map<String, XMPPUserSettings.XMPPUserConfiguration>> userConfigs,
                                  ApplicationServices applicationServices) {
        for(String serviceName : serverConfigs.get().keySet()) {
            XMPPServerSettings.XMPPServerConfiguration serverConfiguration = serverConfigs.get().get(serviceName);
            XMPPUserSettings.XMPPUserConfiguration userConfiguration = userConfigs.get().get(serviceName);
            if(userConfiguration == null) {
                userConfiguration = new XMPPUserSettings.XMPPUserConfiguration(serviceName);
            }
            // TODO per-configuration RosterListeners, XMPPErrorListeners
           
            // the resource is set to a hash of the client guid to uniquely
            // identify the instance of the client
            add(new XMPPConfigurationImpl(serverConfiguration, userConfiguration, null, null, 
                    createHash(applicationServices.getMyGUID())));
        }
    }
    
    private static String createHash(byte[] guid) {
        return StringUtils.getUTF8String(Base64.encodeBase64(new SHA1().digest(guid)));
    }
}
