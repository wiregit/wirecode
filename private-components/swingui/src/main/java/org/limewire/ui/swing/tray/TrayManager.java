package org.limewire.ui.swing.tray;

import org.limewire.util.OSUtils;

import com.google.inject.Singleton;

@Singleton
public class TrayManager {

    /** Whether or not the system tray was able to load. */
    private final boolean LOADED_TRAY_LIBRARY;    
    
    public TrayManager() {
        if (OSUtils.isWindows() || OSUtils.isLinux()) {
            boolean loaded = false;
            try {
                System.loadLibrary("tray");
                loaded = true;
            } catch (UnsatisfiedLinkError ule) {
            }
            LOADED_TRAY_LIBRARY = loaded;
        } else {
            LOADED_TRAY_LIBRARY = false;
        }
    }

    /** Determines if the tray library has loaded. */
    public boolean isTrayLibraryLoaded() {
        return LOADED_TRAY_LIBRARY;
    }
}
