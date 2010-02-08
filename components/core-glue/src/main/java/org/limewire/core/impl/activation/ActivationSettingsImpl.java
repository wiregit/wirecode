package org.limewire.core.impl.activation;

import org.limewire.activation.api.ActSettings;
import org.limewire.core.settings.ActivationSettings;
import org.limewire.setting.evt.SettingListener;
import org.limewire.setting.evt.SettingEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class ActivationSettingsImpl implements ActSettings {

    private final ApplicationServices applicationServices;
    
    static {
        
        // When the application starts, we need to know whether to show the pro or the basic splash screen,
        // and we need to know this before the settings have been loaded. So, we add a listener to this setting
        // that creates a file on disk which we can use to determine whether or not to show the pro splash screen.
        //
        ActivationSettings.LAST_START_WAS_PRO.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                LimeWireUtils.setShouldShowProSplashScreen(
                    ActivationSettings.LAST_START_WAS_PRO.getValue());
            }
        });    
    }
    
    
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
        return ActivationSettings.ACTIVATION_ACCOUNT_SETTINGS_HOST.get();
    }
    
    public String getCustomerSupportHost() {
        return ActivationSettings.ACTIVATION_CUSTOMER_SUPPORT_HOST.get();
    }
    
    public String getDownloadHost() {
        return ActivationSettings.LIMEWIRE_DOWNLOAD_HOST.get();
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
