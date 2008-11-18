package org.limewire.core.impl.xmpp;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.limewire.core.settings.LimeProps;

import com.google.inject.Provider;
import com.google.inject.Singleton;

public class XMPPServerSettings extends LimeProps {
    private static final String[] XMPP_SERVERS =
    {"false,true,talk.google.com,5222,gmail.com,Gmail,http://mail.google.com/mail/signup;" ,
        "false,false,jabber.hot-chilli.net,5222,jabber.hot-chilli.net,Hot-Chilli,http://jabber.hot-chilli.net/jwchat/;" ,
        "false,false,jabberes.org,5222,jabberes.org,JabberES,http://www.jabberes.org/jrt/;" ,
        "false,false,jabber.fr,5222,jabber.fr,JabberFR,http://im.apinc.org/inscription/?server=jabber.fr;" ,
        "false,false,livejournal.com,5222,livejournal.com,LiveJournal,http://www.livejournal.com/create.bml;" ,
        "false,false,macjabber.de,5222,macjabber.de,MacJabber.de,https://macjabber.de:444/;" ,
         };
    
    private XMPPServerSettings() {}
    
//    public static XMPPServerConfiguration add(boolean requiresDomain,
//            String host, Integer port, String serviceName, String friendlyName,
//            String registrationURL) {
//        XMPPServerConfiguration newConfig =
//            new XMPPServerConfiguration(requiresDomain, host, port,
//                    serviceName, friendlyName, registrationURL);  
//        XMPP_SERVERS.add(newConfig.toString());
//        return newConfig;
//    }

    @Singleton
    static class XMPPServerConfigs implements Provider<Map<String, XMPPServerConfiguration>> {

        public Map<String, XMPPServerConfiguration> get() {
            Map<String, XMPPServerConfiguration> configurations = new HashMap<String, XMPPServerConfiguration>();
            for(String connectionString : XMPP_SERVERS) {
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
        private final String registrationURL;
        
        public XMPPServerConfiguration(String connectionString) {
            StringTokenizer st = new StringTokenizer(connectionString, ",");
            isDebugEnabled = Boolean.valueOf(st.nextToken());
            requiresDomain = Boolean.valueOf(st.nextToken());
            host = st.nextToken();
            port = Integer.valueOf(st.nextToken());
            serviceName = st.nextToken();
            friendlyName = st.nextToken();
            registrationURL = st.nextToken();
        }

        public XMPPServerConfiguration(boolean requiresDomain, String host,
                int port, String serviceName, String friendlyName,
                String registrationURL) {
            this.requiresDomain = requiresDomain;
            this.host = host;
            this.port = port;
            this.serviceName = serviceName;
            this.friendlyName = friendlyName;
            this.registrationURL = registrationURL;
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
        
        public String getRegistrationURL() {
            return registrationURL;
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
