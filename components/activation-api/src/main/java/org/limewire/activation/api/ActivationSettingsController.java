package org.limewire.activation.api;

public interface ActivationSettingsController {

    public static final String ACCOUNT_SETTINGS_URL = "http://www.limewire.com/client_redirect/?page=accountDetails";
    public static final String CUSTOMER_SUPPORT_URL = "http://www.limewire.com/client_redirect/?page=proSupport";
    public static final String UPSELL_URL = "http://www.limewire.com/client_redirect/?page=downloadPro";
    public static final String RENEW_URL = "http://www.limewire.com/client_redirect/?page=renewPro";

    public String getActivationHost();

    public boolean isLastStartPro();
    
    public boolean isNewInstall();
    
    public void setLastStartPro(boolean value);

    public String getActivationKey();
    
    public void setActivationKey(String key);
    
    public String getModuleCode();
    
    public void setModuleCode(String moduleCode);
    
    public String getPassKey();
    
    public String getServerKey();
    
    public String getQueryString();
}
