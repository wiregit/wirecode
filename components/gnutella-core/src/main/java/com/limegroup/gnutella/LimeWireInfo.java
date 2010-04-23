package com.limegroup.gnutella;

import org.limewire.activation.api.ActivationManager;
import org.limewire.http.httpclient.HttpClientInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * TODO: this class was created to move the bidning of httpClientInstanceUtils into
 * core rather than core-glue. It was being bound in ApplicationImpl which  
 * duplicates this code. With Activation, SimppManagerImpl needs access to this
 * so it can no longer be bound in core-glue. Should readdress all this and just 
 * bind it once and reuse that class.
 */
@Singleton
public class LimeWireInfo implements HttpClientInstanceUtils {

    private final ApplicationServices applicationServices;
    private final ActivationManager activationManager;
    
    @Inject
    public LimeWireInfo(ApplicationServices applicationServices, ActivationManager activationManager) {
        this.applicationServices = applicationServices;
        this.activationManager = activationManager;
    }

    @Override
    public String addClientInfoToUrl(String baseUrl) {
        return LimeWireUtils.addLWInfoToUrl(baseUrl, applicationServices.getMyGUID(),
                activationManager.isProActive(), activationManager.getModuleCode());
    }    
}
