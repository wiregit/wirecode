package org.limewire.core.impl;

import org.limewire.core.api.Application;


public class MockApplication implements Application {
    
    @Override
    public String getUniqueUrl(String baseUrl) {
        return baseUrl;
    }
    
    @Override
    public void startCore() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void stopCore() {
        // TODO Auto-generated method stub
        
    }
    
}
