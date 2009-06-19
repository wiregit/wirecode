package org.limewire.ui.swing.friends.login;

import javax.swing.SwingUtilities;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AutoLoginService implements Service {
    
    private final XMPPAccountConfigurationManager accountManager;
    private final XMPPService service;
    private boolean hasAttemptedLogin = false;
    
    @Inject
    public AutoLoginService(XMPPAccountConfigurationManager accountManager, XMPPService service) {
        this.accountManager = accountManager;
        this.service = service;
    }
    
    /**
     * Used to identify whether or not this service will attempt to automatically login.
     */
    public boolean hasLoginConfig() {
        return accountManager.getAutoLoginConfig() != null;
    }
    
    /**
     * Whether or not the service has attempted a login yet.
     */
    public boolean hasAttemptedLogin() {
        return hasAttemptedLogin;
    }
    
    /**
     * If an auto login is in process.
     */
    public boolean isAttemptingLogin() {
        return !hasAttemptedLogin() && hasLoginConfig(); 
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return "Auto-Login Serivce";
    }
    @Override
    public void initialize() {
    }
    
    @Override
    public void start() {
        // If there's an auto-login account, select it and log in
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(hasLoginConfig()) {
                    service.login(accountManager.getAutoLoginConfig());
                }
                hasAttemptedLogin = true;
            }
        });
    }
    
    @Override
    public void stop() {
    }
}
