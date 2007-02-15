package com.limegroup.gnutella;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.security.QueryKeySmith;
import org.limewire.security.SettingsProvider;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.settings.SecuritySettings;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class is the glue that holds LimeWire together.
 * All various components are wired together here.
 */
public class LimeCoreGlue {
    
    private static AtomicBoolean preinstalled = new AtomicBoolean(false);
    private static AtomicBoolean installed = new AtomicBoolean(false);
    
    private LimeCoreGlue() {}
    
    /** Wires initial pieces together that are required for nearly everything. */
    public static void preinstall() {
        // Only preinstall once
        if(!preinstalled.compareAndSet(false, true))
            return;
        
        try {
            CommonUtils.setUserSettingsDir(LimeWireUtils.getRequestedUserSettingsLocation());
        } catch(IOException iox) {
            // If the settings directory cannot be created, bail.
            throw new RuntimeException(iox);
        }
    }

    /** Wires all various components together. */
    public static void install() {
        // Only install once.
        if(!installed.compareAndSet(false, true))
            return;
        
        preinstall(); // Ensure we're preinstalled.
                
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

            public long getGracePeriod() {
                return SecuritySettings.QK_GRACE_PERIOD.getValue();
            }
        };
        
        QueryKeySmith.setSettingsProvider(settingsProvider);
    }

}
