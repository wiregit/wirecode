package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.util.LimeWireUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;

@Singleton
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
