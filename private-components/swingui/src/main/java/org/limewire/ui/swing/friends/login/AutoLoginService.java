package org.limewire.ui.swing.friends.login;

import javax.swing.SwingUtilities;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AutoLoginService implements Service {
    
    private final XMPPAccountConfigurationManager accountManager;
    private final XMPPService service;
    
    @Inject
    public AutoLoginService(XMPPAccountConfigurationManager accountManager, XMPPService service) {
        this.accountManager = accountManager;
        this.service = service;
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
                XMPPAccountConfiguration autoConf =
                    accountManager.getAutoLoginConfig();
                if(autoConf != null) {
                    service.login(autoConf);
                }
            }
        });
    }
    
    @Override
    public void stop() {
    }
}
