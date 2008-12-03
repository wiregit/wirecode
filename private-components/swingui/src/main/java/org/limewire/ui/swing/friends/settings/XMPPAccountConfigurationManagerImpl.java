package org.limewire.ui.swing.friends.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.xmpp.api.client.PasswordManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPAccountConfigurationManagerImpl implements XMPPAccountConfigurationManager {
    
    private final PasswordManager passwordManager;
    private HashMap<String,XMPPAccountConfiguration> configs; // Indexed by label
    private XMPPAccountConfiguration autoLoginConfig = null;
    private final String resource;
    
    @Inject
    public XMPPAccountConfigurationManagerImpl(PasswordManager passwordManager,
            XMPPResourceFactory xmppResourceFactory) {
        this.passwordManager = passwordManager;
        configs = new HashMap<String,XMPPAccountConfiguration>();
        resource = xmppResourceFactory.getResource();
        for(String server : XMPPSettings.XMPP_SERVERS.getValue()) {
            try {
                XMPPAccountConfiguration config =
                    new XMPPAccountConfigurationImpl(server, resource);
                configs.put(config.getLabel(), config);
            } catch(IllegalArgumentException ignored) {
                // Malformed string - no soup for you!
            }
        }
        String autoLogin = XMPPSettings.XMPP_AUTO_LOGIN.getValue();
        if(!autoLogin.equals("")) {
            int comma = autoLogin.indexOf(',');
            try {
                String label = autoLogin.substring(0, comma);
                String username = autoLogin.substring(comma + 1);
                String password = passwordManager.loadPassword(username);
                XMPPAccountConfiguration config = configs.get(label);
                if(config != null) {
                    config.setUsername(username);
                    config.setPassword(password);
                    autoLoginConfig = config;
                }
            } catch(IndexOutOfBoundsException ignored) {
                // Malformed string - no soup for you!
            } catch(IllegalArgumentException ignored) {
                // Empty username - no soup for you!
            } catch(IOException ignored) {
                // Error decrypting password - no soup for you!
            }
        }
    }
    
    public XMPPAccountConfiguration getConfig(String label) {
        return configs.get(label);
    }
    
    public List<String> getLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for(XMPPAccountConfiguration config : configs.values())
            labels.add(config.getLabel());
        Collections.sort(labels);
        return labels;
    }
    
    public XMPPAccountConfiguration getAutoLoginConfig() {
        return autoLoginConfig;
    }
    
    public void setAutoLoginConfig(XMPPAccountConfiguration config) {
        // Remove the old configuration, if there is one
        if(autoLoginConfig != null) {
            passwordManager.removePassword(autoLoginConfig.getUsername());
            XMPPSettings.XMPP_AUTO_LOGIN.setValue("");
            autoLoginConfig = null;
        }
        // Store the new configuration, if there is one
        if(config != null) {
            try {
                passwordManager.storePassword(config.getUsername(),
                        config.getPassword());
                XMPPSettings.XMPP_AUTO_LOGIN.setValue(config.getLabel() + "," +
                        config.getUsername());
                autoLoginConfig = config;
            } catch (IllegalArgumentException ignored) {
                // Empty username or password - no soup for you!
            } catch (IOException ignored) {
                // Error encrypting password - no more Soup Nazi jokes for you!
            }
        }
    }
}
