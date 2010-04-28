package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationSettingsController;

public class ActivationSettingStub implements ActivationSettingsController {

    private String activationKey = "";
    private String mcode = "";
    private String activationHost = "http://activation.limewire.int";
    String serverPublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC20OomYqv79KOq0ueRS5+OtTBQIMoqWY8lXdG6L8" +
        "KUXj4ysNgEbS2ChsixLr7r3aLTuOjz2cRqNNbHBqaB95KOpjKANVjJ5t3jxcXfux+ilwdJhqjx+TdY6tCukl0i3oq/9or9kafBZ" +
        "73CY29rDUKeoWE0OwPrcPvvy5AXJiVofwIDAQAB";
    
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
    public String getServerKey() {
        return serverPublicKey;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public void setActivationKey(String key) {
        this.activationKey = key;
    }

    @Override
    public boolean isLastStartPro() {
        return false;
    }

    @Override
    public boolean isNewInstall() {
        return false;
    }

    @Override
    public void setLastStartPro(boolean value) {
//        this.isLastStartPro = value;
    }

    @Override
    public void setModuleCode(String mCode) {
        this.mcode = mCode;
    }
}
