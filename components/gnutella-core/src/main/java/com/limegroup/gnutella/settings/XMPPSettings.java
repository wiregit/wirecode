package com.limegroup.gnutella.settings;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.xmpp.XMPPServerConfiguration;
import org.limewire.setting.StringSetSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

@Singleton
public class XMPPSettings extends LimeProps implements Provider<List<XMPPServerConfiguration>> {
    private static final StringSetSetting XMPP_CONNECTIONS =
		FACTORY.createStringSetSetting("XMPP_CONNECTIONS", null);

    public List<XMPPServerConfiguration> get() {
        List<XMPPServerConfiguration> configurations = new ArrayList<XMPPServerConfiguration>();
        Set<String> connectionsString = XMPP_CONNECTIONS.getValue();
        for(String connectionString : connectionsString) {
            StringTokenizer st = new StringTokenizer(connectionString, " ");
            boolean isDebugEnabled = Boolean.valueOf(st.nextToken());
            String username = st.nextToken();
            String password = st.nextToken();
            String host = st.nextToken();
            int port = Integer.valueOf(st.nextToken());
            String serviceName = st.nextToken();
            boolean isAutoLogin = Boolean.valueOf(st.nextToken());
            configurations.add(new XMPPServerConfiguration(isDebugEnabled,
                    username, password, host, port, serviceName, isAutoLogin));
        }
        return configurations;
    }
}
