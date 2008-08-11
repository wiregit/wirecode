package org.limewire.core.impl.xmpp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.StringSetSetting;
import org.limewire.util.Objects;

import com.google.inject.Provider;
import com.google.inject.Singleton;

public class XMPPUserSettings extends LimeProps {
     private static final StringSetSetting XMPP_USERS =
		FACTORY.createStringSetSetting("XMPP_USERS", "");
    
    private XMPPUserSettings() {}
    
    @Singleton
    static class XMPPUserConfigs implements Provider<Map<String, XMPPUserConfiguration>> {

        public Map<String, XMPPUserConfiguration> get() {
            Map<String, XMPPUserConfiguration> configurations = new HashMap<String, XMPPUserConfiguration>();
            Set<String> connectionsString = XMPP_USERS.getValue();
            for(String connectionString : connectionsString) {
                XMPPUserConfiguration config = new XMPPUserConfiguration(connectionString);
                configurations.put(config.getServiceName(), config);
            }
            return configurations;
        }
    }
    
    public static class XMPPUserConfiguration {
        private final String serviceName;
        private String username;
        private String password;
        private boolean isAutoLogin;
        
        public XMPPUserConfiguration(String connectionString) {
            StringTokenizer st = new StringTokenizer(connectionString, " ");
            serviceName = st.nextToken();
            if(st.hasMoreTokens()) {
                username = st.nextToken();
                password = st.nextToken(); 
                isAutoLogin = true;
            } else {
                isAutoLogin = false;
            }
        }

        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            if(!Objects.equalOrNull(this.username, username)) {
                if(isAutoLogin()) {
                    XMPP_USERS.remove(this.toString());
                }
                this.username = username;
                if(isAutoLogin()) {
                    XMPP_USERS.add(this.toString());
                    FACTORY.save();
                }
            }
        }

        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            if(!Objects.equalOrNull(this.password, password)) {
                if(isAutoLogin()) {
                    XMPP_USERS.remove(this.toString());
                }
                this.password = password;
                if(isAutoLogin()) {
                    XMPP_USERS.add(this.toString());
                    FACTORY.save();
                }
            }
        }

        public String getServiceName() {
            return serviceName;
        }
        
        public boolean isAutoLogin() {
            return isAutoLogin;
        }

        public void setAutoLogin(boolean autoLogin) {
            if(!Objects.equalOrNull(this.isAutoLogin, autoLogin)) {
                XMPP_USERS.remove(this.toString());
                this.isAutoLogin = autoLogin;
                XMPP_USERS.add(this.toString());
                FACTORY.save();
            }
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(serviceName);
            if(username != null) {
                sb.append(' ');
                sb.append(username);
            }
            if(password != null) {
                sb.append(' ');
                sb.append(password);
            }
            return sb.toString();
        }
    }
}
