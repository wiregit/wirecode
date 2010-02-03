package org.limewire.core.impl;

import org.limewire.core.api.Application;

/**
 * Implementation of Application for the mock core.
 */
public class MockApplication implements Application {
    
    @Override
    public String addClientInfoToUrl(String baseUrl) {
        if (baseUrl.indexOf('?') == -1)
            baseUrl += "?";
        else
            baseUrl += "&";
        baseUrl += "lang=en";
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
    
    @Override
    public void setShutdownFlag(String flag) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean isTestingVersion() {
        // TODO Auto-generated method stub
        return true;
    }    
    
    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public boolean isProVersion() {
        return false;
    }
    
    @Override
    public boolean isBetaVersion() {
        return true;
    }

    @Override
    public boolean isNewInstall() {
        return false;
    }

    @Override
    public boolean isNewJavaVersion() {
        return false;
    }
    
}
