package org.limewire.activation.impl;

import org.limewire.activation.api.ActSettings;

public class ActivationSettingStub implements ActSettings {

    @Override
    public String getActivationHost() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getActivationKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getActivationRenewalHost() {
        return "http://activation.limewire.com";
    }

    @Override
    public String getMCode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPassKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQueryString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLastStartPro() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setActivationKey(String key) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLastStartPro(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMCode(String mCode) {
        // TODO Auto-generated method stub
        
    }

}
