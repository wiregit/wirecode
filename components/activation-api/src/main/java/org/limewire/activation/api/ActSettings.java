package org.limewire.activation.api;

public interface ActSettings {

    public String getActivationHost();
    
    public String getActivationRenewalHost();
    
    public boolean isLastStartPro();
    
    public void setLastStartPro(boolean value);
    
    public String getActivationKey();
    
    public void setActivationKey(String key);
    
    public String getMCode();
    
    public void setMCode(String mCode);
    
    public String getPassKey();
    
    public String getQueryString();
}
