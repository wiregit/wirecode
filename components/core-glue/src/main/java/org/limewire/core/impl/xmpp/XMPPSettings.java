package org.limewire.core.impl.xmpp;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.LimeProps;
import org.limewire.setting.StringSetSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class XMPPSettings extends LimeProps {
    private static final StringSetSetting XMPP_CONNECTIONS =
		FACTORY.createStringSetSetting("XMPP_CONNECTIONS", "");
    
    private XMPPSettings() {}

    @Singleton
    static class XMPPServerConfigs implements Provider<List<XMPPServerConfiguration>> {

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

    public static class XMPPServerConfiguration {
        private final boolean isDebugEnabled;
        private final String username;
        private final String password;
        private final String host;
        private final int port;
        private final String serviceName;
        private final boolean isAutoLogin;

        public XMPPServerConfiguration(boolean debugEnabled,
                                       String username,
                                       String password,
                                       String host,
                                       int port,
                                       String serviceName,
                                       boolean autoLogin) {
            isDebugEnabled = debugEnabled;
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
            isAutoLogin = autoLogin;
        }

        public boolean isDebugEnabled() {
            return isDebugEnabled;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getServiceName() {
            return serviceName;
        }

        public boolean isAutoLogin() {
            return isAutoLogin;
        }
    }
}
