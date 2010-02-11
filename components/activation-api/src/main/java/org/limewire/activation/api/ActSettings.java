package org.limewire.activation.api;

public interface ActSettings {

    public String getActivationHost();
    
    public String getActivationRenewalHost();

    public String getCustomerSupportHost();
    
    public boolean isLastStartPro();
    
    public void setLastStartPro(boolean value);

    public String getActivationKey();
    
    public void setActivationKey(String key);
    
    public String getModuleCode();
    
    public void setModuleCode(String moduleCode);
    
    public String getPassKey();
    
    public String getQueryString();
}
