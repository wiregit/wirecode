package org.limewire.mojito2;

import org.limewire.mojito2.util.ContactUtils;

import com.google.inject.AbstractModule;

public class LimeWireMojitoModule extends AbstractModule {

    @Override
    protected void configure() {
        requestStaticInjection(ContactUtils.class);
    }
    
}