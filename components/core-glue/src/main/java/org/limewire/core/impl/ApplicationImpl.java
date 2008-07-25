package org.limewire.core.impl;

import org.limewire.core.api.Application;

import com.google.inject.Inject;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.LimeWireUtils;

class ApplicationImpl implements Application {
    
    private final ApplicationServices applicationServices;
    
    @Inject
    public ApplicationImpl(ApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
    }
    
    @Override
    public String getUniqueUrl(String baseUrl) {
        return LimeWireUtils.addLWInfoToUrl(baseUrl, applicationServices.getMyGUID());
    }

}
