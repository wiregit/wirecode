package org.limewire.core.impl.xmpp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.StringSetSetting;
import org.limewire.util.Objects;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

public class XMPPUserSettings extends LimeProps {
     private static final StringSetSetting XMPP_USERS =
		FACTORY.createStringSetSetting("XMPP_USERS", "");

    private static PasswordManager passwordManager;
    
    private XMPPUserSettings() {}
    
    @Singleton
    static class XMPPUserConfigs implements Provider<Map<String, XMPPUserConfiguration>> {

        @Inject
        public XMPPUserConfigs(PasswordManager passwordManager) {
            XMPPUserSettings.passwordManager = passwordManager;
        }

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

        private static final Log LOG = LogFactory.getLog(XMPPUserSettings.XMPPUserConfiguration.class);

        private final String serviceName;
        private String username;
        private String password;
        private boolean isAutoLogin;
        private final String originalConnectionString;
        private boolean logonDetailsChanged;

        public XMPPUserConfiguration(String connectionString) {
            originalConnectionString = connectionString;
            isAutoLogin = false;
            password = null;
            logonDetailsChanged = false;

            StringTokenizer st = new StringTokenizer(connectionString, " ");
            serviceName = st.nextToken();
            if(st.hasMoreTokens()) {
                username = st.nextToken();

                try {
                    password = passwordManager.loadPasswordFromUserName(username);
                    isAutoLogin = true;
                } catch (XmppEncryptionException e) {
                    // TODO: Perform any necessary error handling related to indicating to all
                    // concerned parties that this is an invalid config!
                    LOG.warn("Error loading password.", e);
                }
            }
        }

        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            if(!Objects.equalOrNull(this.username, username)) {
                this.username = username;
                this.logonDetailsChanged = true;
            }
        }

        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            if(!Objects.equalOrNull(this.password, password)) {
                this.password = password;
                this.logonDetailsChanged = true;
            }
        }

        public String getServiceName() {
            return serviceName;
        }
        
        public boolean isAutoLogin() {
            return isAutoLogin;
        }

        public void setAutoLogin(boolean autoLogin) {
            if (logonDetailsChanged || (this.isAutoLogin != autoLogin)) {
                XMPP_USERS.remove(originalConnectionString);
                this.isAutoLogin = autoLogin;
                if (isAutoLogin) {
                    try {
                        passwordManager.storePassword(username, password);
                    } catch (XmppEncryptionException e) {
                        // TODO: Call an error listener to notify a component of the error
                        // Currently, the error will be discovered when the xmppService tries to autologin
                    }
                    XMPP_USERS.add(this.toString());
                } else {
                    passwordManager.removePassword(username);
                }
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
            return sb.toString();
        }
    }
}
