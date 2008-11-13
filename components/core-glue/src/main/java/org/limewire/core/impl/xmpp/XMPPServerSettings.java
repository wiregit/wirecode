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
        FACTORY.createStringSetSetting("XMPP_SERVERS",
        //"false,false,im.apinc.org,5222,im.apinc.org,APINC;" +
        //"false,false,jabber.ccc.de,5222,jabber.ccc.de,Chaos Computer Club;" +
        //"false,false,darkdna.net,5222,darkdna.net,DarkDNA;" +
        //"false,false,im.flosoft.biz,5222,im.flosoft.biz,Flosoft.biz;" +
        "false,true,talk.google.com,5222,gmail.com,Gmail;" +
        //"false,false,jabber.hot-chilli.net,5222,jabber.hot-chilli.net,Hot-Chilli;" +
        //"false,false,jabber.org,5222,jabber.org,Jabber.org;" +
        //"false,false,jabber.se,5222,jabber.se,Jabber.se;" +
        //"false,false,jabberes.org,5222,jabberes.org,JabberES;" +
        //"false,false,jabbim.com,5222,jabbim.com,Jabbim;" +
        //"false,false,jabster.pl,5222,jabster.pl,Jabster.pl;" +
        "false,false,livejournal.com,5222,livejournal.com,LiveJournal;" +
        //"false,false,macjabber.de,5222,macjabber.de,MacJabber.de;" +
        //"false,false,programmer-art.org,5222,programmer-art.org,Programmer-Art;" +
        "");
    
    private XMPPServerSettings() {}
    
    public static XMPPServerConfiguration add(boolean requiresDomain,
                                    String host,  Integer port,
                                    String serviceName, String friendlyName) {
        XMPPServerConfiguration newConfig =
            new XMPPServerConfiguration(requiresDomain, host, port, serviceName, friendlyName);  
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
            return configurations;
        }
    }

    public static class XMPPServerConfiguration {
        private final boolean isDebugEnabled;
        private final boolean requiresDomain;
        private final String host;
        private final int port;
        private final String serviceName;
        private final String friendlyName;
        
        public XMPPServerConfiguration(String connectionString) {
            StringTokenizer st = new StringTokenizer(connectionString, ",");
            isDebugEnabled = Boolean.valueOf(st.nextToken());
            requiresDomain = Boolean.valueOf(st.nextToken());
            host = st.nextToken();
            port = Integer.valueOf(st.nextToken());
            serviceName = st.nextToken();
            friendlyName = st.nextToken();
        }

        public XMPPServerConfiguration(boolean requiresDomain, String host,
                                        int port, String serviceName,
                                        String friendlyName) {
            this.requiresDomain = requiresDomain;
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
            this.friendlyName = friendlyName;
            isDebugEnabled = false;
        }

        public boolean isDebugEnabled() {
            return isDebugEnabled;
        }

        public boolean requiresDomain() {
            return requiresDomain;
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
        
        public String getFriendlyName() {
            return friendlyName;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Boolean.toString(isDebugEnabled));
            sb.append(' ');
            sb.append(Boolean.toString(requiresDomain));
            sb.append(' ');
            sb.append(host);
            sb.append(' ');
            sb.append(Integer.toString(port));
            sb.append(' ');
            sb.append(serviceName);
            sb.append(' ');
            sb.append(friendlyName);
            return sb.toString();
        }
    }
}
