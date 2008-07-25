package org.limewire.core.impl;

import org.limewire.core.api.Application;


public class MockApplication implements Application {
    
    @Override
    public String getUniqueUrl(String baseUrl) {
        return baseUrl;
    }
}
