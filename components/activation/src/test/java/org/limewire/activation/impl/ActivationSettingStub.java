package org.limewire.activation.impl;

import org.limewire.activation.api.ActSettings;

public class ActivationSettingStub implements ActSettings {

    private String activationKey = "";
    private String mcode = "";
    private boolean isLastStartPro = false;
    
    @Override
    public String getActivationHost() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getActivationKey() {
        return activationKey;
    }

    @Override
    public String getActivationRenewalHost() {
        return "http://activation.limewire.com";
    }

    @Override
    public String getMCode() {
        return mcode;
    }

    @Override
    public String getPassKey() {
        return "3A931AF193AC44F66540CFFC57C3978D";
    }

    @Override
    public String getQueryString() {
        // TODO Auto-generated method stub
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
    public void setMCode(String mCode) {
        this.mcode = mCode;
    }

    @Override
    public String getCustomerSupportHost() {
        return "http://www.limewire.com/support";
    }

    @Override
    public String getDownloadHost() {
        return "http://www.limewire.com/download";
    }
}
