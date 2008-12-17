package org.limewire.ui.swing.wizard;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;

/**
 * Helper class that provides methods for determining the correct
 *  autoconfig values for certain limewire settings related
 *  to the directory manager.
 */
public class AutoDirectoryManageConfig {
    
    /**
     * Whether scanning files should be turned on by default 
     */
    public static boolean shouldScanFiles() {
        return true;
    }
    
    /**
     * Determines the OS specific list of directories to manage by default
     */
    public static Set<File> getManagedDirectories() {
        Set<File> dirs = new HashSet<File>();

        if (OSUtils.isWindows()) {
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS)));
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP)));
        } else {
        
            String homePath = SystemUtils.getSpecialPath(SpecialLocations.HOME);
            
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS)));
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP)));
            dirs.add(new File(homePath + "/Downloads"));
            dirs.add(new File(homePath + "/Movies"));
            dirs.add(new File(homePath + "/Music"));
            dirs.add(new File(homePath + "/Pictures"));
            dirs.add(new File(homePath + "/Public"));
        }
                
        return dirs;
    }
    
    /**
     * Determines the OS specific list of directories to exclude
     */
    public static  Set<File> getExcludedDirectories() {
        Set<File> dirs = new HashSet<File>();
                
        return dirs;
    }
}
