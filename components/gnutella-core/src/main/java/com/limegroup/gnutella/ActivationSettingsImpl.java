package com.limegroup.gnutella;

import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.core.settings.ActivationSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class ActivationSettingsImpl implements ActivationSettingsController {

    private static final String ACTIVATION_HOST = "https://activate.limewire.com/lookup";
    private static final String CACHE_KEY = "3A931AF193AC44F66540CFFC57C3978D";
    
    private final ApplicationServices applicationServices;
    
    @Inject
    public ActivationSettingsImpl(ApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
    }
    
    @Override
    public String getActivationHost() {
        return ACTIVATION_HOST;
    }

    @Override
    public String getActivationKey() {
        return ActivationSettings.ACTIVATION_KEY.get();
    }

    @Override
    public String getModuleCode() {
        return ActivationSettings.MODULE_CODE.get();
    }

    @Override
    public String getPassKey() {
        return CACHE_KEY;
    }

    @Override
    public void setActivationKey(String key) {
        ActivationSettings.ACTIVATION_KEY.set(key);
    }

    public boolean isLastStartPro() {
        return ActivationSettings.LAST_START_WAS_PRO.getValue();
    }
    
    public boolean isNewInstall() {
        return this.applicationServices.isNewInstall();
    }

    @Override
    public void setLastStartPro(boolean value) {
        ActivationSettings.LAST_START_WAS_PRO.setValue(value);
        LimeWireUtils.setIsPro(value);
    }

    @Override
    public void setModuleCode(String mCode) {
        ActivationSettings.MODULE_CODE.set(mCode);
    }
    
    @Override
    public String getQueryString() {
        return LimeWireUtils.getLWInfoQueryString(applicationServices.getMyGUID(), 
                        ActivationSettings.LAST_START_WAS_PRO.getValue(), getModuleCode());
    }
}
