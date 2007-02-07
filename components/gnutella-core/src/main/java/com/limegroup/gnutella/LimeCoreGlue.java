package com.limegroup.gnutella;

import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.security.QueryKeySmith;
import org.limewire.security.SettingsProvider;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.settings.SecuritySettings;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * This class is the glue that holds LimeWire together.
 * All various components are wired together here.
 */
public class LimeCoreGlue {
    
    private static AtomicBoolean glued = new AtomicBoolean(false);
    
    private LimeCoreGlue() {}

    /** Wires all various components together. */
    public static void install() {
        // Only glue once.
        if(!glued.compareAndSet(false, true))
            return;
                
        // Setup SIMPP to be the settings remote manager.
        SimppManager simppManager = SimppManager.instance();
        SimppSettingsManager settingsManager = SimppSettingsManager.instance();
        LimeProps.instance().getFactory().setRemoteSettingManager(settingsManager);
        settingsManager.updateSimppSettings(simppManager.getPropsString());
        
        // Setup RouterService & ConnectionSettings.LOCAL_IS_PRIVATE to 
        // be the LocalSocketAddressProvider.
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProvider() {
            public byte[] getLocalAddress() {
                return RouterService.getAddress();
            }

            public int getLocalPort() {
                return RouterService.getPort();
            }

            public boolean isLocalAddressPrivate() {
                return ConnectionSettings.LOCAL_IS_PRIVATE.getValue();
            }
        });
        
        SettingsProvider settingsProvider = new SettingsProvider() {
            public long getChangePeriod() {
                return SecuritySettings.CHANGE_QK_EVERY.getValue();
            }

            public long getGrancePeriod() {
                return SecuritySettings.QK_GRACE_PERIOD.getValue();
            }
        };
        
        QueryKeySmith.setSettingsProvider(settingsProvider);
    }

}
