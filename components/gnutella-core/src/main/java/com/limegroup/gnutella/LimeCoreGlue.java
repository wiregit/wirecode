package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.security.QueryKey;
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
        
        // This looks a lot more complicated than it really is.
        // The excess try/catch blocks are just to make debugging easier,
        // to keep track of what messages each successive IOException is.
        // The flow is basically:
        //  - Try to set the settings dir to the requested location.
        //  - If that doesn't work, try getting a temporary directory to use.
        //  - If we can't find a temporary directory, deleting old stale ones & try again.
        //  - If it still doesn't work, bail.
        //  - If it did work, mark it for deletion & set it as the settings directory.
        //  - If it can't be set, bail.
        //  - Otherwise, success.
        try {
            CommonUtils.setUserSettingsDir(LimeWireUtils.getRequestedUserSettingsLocation());
        } catch(IOException iox) {
            try {
                File temporaryDir;
                try {
                    temporaryDir = LimeWireUtils.getTemporarySettingsDirectory();
                } catch(IOException tempFull) {
                    tempFull.printStackTrace();
                    tempFull.initCause(iox);
                    LimeWireUtils.clearTemporarySettingsDirectories();
                    try {
                        temporaryDir = LimeWireUtils.getTemporarySettingsDirectory();
                    } catch(IOException stillBad) {
                        stillBad.initCause(tempFull);
                        throw stillBad;
                    }
                }
                temporaryDir.deleteOnExit();
                try {
                    CommonUtils.setUserSettingsDir(temporaryDir);
                } catch(IOException cannotSet) {
                    cannotSet.initCause(iox);
                    throw cannotSet;
                }
                
                LimeWireUtils.setTemporaryDirectoryInUse(true);
            } catch(IOException reallyBad) {
                // If the settings directory cannot be created, bail.
                throw new RuntimeException(reallyBad);
            }
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
        
        QueryKey.SettingsProvider settingsProvider = new QueryKey.SettingsProvider() {
            public long getChangePeriod() {
                return SecuritySettings.CHANGE_QK_EVERY.getValue();
            }

            public long getGrancePeriod() {
                return SecuritySettings.QK_GRACE_PERIOD.getValue();
            }
        };
        
        QueryKey.setSettingsProvider(settingsProvider);
    }

}
