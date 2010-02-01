package org.limewire.core.impl.activation;

import org.limewire.activation.api.ActSettings;
import org.limewire.core.settings.ActivationSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class ActivationSettingsImpl implements ActSettings {

    private final ApplicationServices applicationServices;
    
    @Inject
    public ActivationSettingsImpl(ApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
    }
    
    @Override
    public String getActivationHost() {
        return ActivationSettings.ACTIVATION_HOST.get();
    }

    @Override
    public String getActivationRenewalHost() {
        return ActivationSettings.ACTIVATION_RENEWAL_HOST.get() + "?" + ActivationSettings.MODULE_KEY_IDENTIFIER.get();
    }
    
    @Override
    public String getActivationKey() {
        return ActivationSettings.ACTIVATION_KEY.get();
    }

    @Override
    public String getMCode() {
        return ActivationSettings.M_CODE.get();
    }

    @Override
    public String getPassKey() {
        return ActivationSettings.PASS_KEY.get();
    }

    @Override
    public boolean isLastStartPro() {
        return ActivationSettings.LAST_START_WAS_PRO.getValue();
    }

    @Override
    public void setActivationKey(String key) {
        ActivationSettings.ACTIVATION_KEY.set(key);
    }

    @Override
    public void setLastStartPro(boolean value) {
        ActivationSettings.LAST_START_WAS_PRO.setValue(value);
        LimeWireUtils.setIsPro(value);
    }

    @Override
    public void setMCode(String mCode) {
        ActivationSettings.M_CODE.set(mCode);
    }
    
    @Override
    public String getQueryString() {
        return LimeWireUtils.getLWInfoQueryString(applicationServices.getMyGUID(), isLastStartPro(), getMCode());
    }
}
