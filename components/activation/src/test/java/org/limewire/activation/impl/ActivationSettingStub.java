package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationSettingsController;

public class ActivationSettingStub implements ActivationSettingsController {

    private String activationKey = "";
    private String mcode = "";
    private boolean isLastStartPro = false;
    private String activationHost = "http://activation.limewire.int";
    
    @Override
    public String getActivationHost() {
        return activationHost;
    }
    
    void setActivationHost(String activationHostParam) {
        activationHost = activationHostParam;
    }

    @Override
    public String getActivationKey() {
        return activationKey;
    }

    @Override
    public String getModuleCode() {
        return mcode;
    }

    @Override
    public String getPassKey() {
        return "3A931AF193AC44F66540CFFC57C3978D";
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public boolean isLastStartPro() {
        return isLastStartPro;
    }

    @Override
    public void setActivationKey(String key) {
        this.activationKey = key;
    }

    @Override
    public void setLastStartPro(boolean value) {
        this.isLastStartPro = value;
    }

    @Override
    public void setModuleCode(String mCode) {
        this.mcode = mCode;
    }
}
