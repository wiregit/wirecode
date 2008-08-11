package org.limewire.core.impl.xmpp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.StringSetSetting;

import com.google.inject.Provider;
import com.google.inject.Singleton;

public class XMPPServerSettings extends LimeProps {
    private static final StringSetSetting XMPP_SERVERS =
		FACTORY.createStringSetSetting("XMPP_SERVERS", "");
    
    private XMPPServerSettings() {}
    
    public static XMPPServerConfiguration add(String host, Integer port, String serviceName) {
        XMPPServerConfiguration newConfig = new XMPPServerConfiguration(host, port, serviceName);  
        XMPP_SERVERS.add(newConfig.toString());
        return newConfig;
    }

    @Singleton
    static class XMPPServerConfigs implements Provider<Map<String, XMPPServerConfiguration>> {

        public Map<String, XMPPServerConfiguration> get() {
            Map<String, XMPPServerConfiguration> configurations = new HashMap<String, XMPPServerConfiguration>();
            Set<String> connectionsString = XMPP_SERVERS.getValue();
            for(String connectionString : connectionsString) {
                XMPPServerConfiguration config = new XMPPServerConfiguration(connectionString);
                configurations.put(config.getServiceName(), config);
            }
            // TODO move to resources file
            configurations.put("gmail.com", new XMPPServerConfiguration("talk.google.com", 5222, "gmail.com"));
            return configurations;
        }
    }

    public static class XMPPServerConfiguration {
        private final Boolean isDebugEnabled;
        private final String host;
        private final Integer port;
        private final String serviceName;
        
        public XMPPServerConfiguration(String connectionString) {
            StringTokenizer st = new StringTokenizer(connectionString, " ");
            isDebugEnabled = Boolean.valueOf(st.nextToken());
            host = st.nextToken();
            port = Integer.valueOf(st.nextToken());
            serviceName = st.nextToken();
        }

        public XMPPServerConfiguration(String host, Integer port, String serviceName) {
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
            isDebugEnabled = true;
        }

        public boolean isDebugEnabled() {
            return isDebugEnabled;
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
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(isDebugEnabled.toString());
            sb.append(' ');
            sb.append(host);
            sb.append(' ');
            sb.append(port.toString());
            sb.append(' ');
            sb.append(serviceName);
            return sb.toString();
        }
    }
}
