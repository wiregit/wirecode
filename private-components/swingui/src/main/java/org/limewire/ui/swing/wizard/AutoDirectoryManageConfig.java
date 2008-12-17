package org.limewire.ui.swing.wizard;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

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
    public static Collection<File> getManagedDirectories() {
        Collection<File> dirs = new LinkedList<File>();
        
        if (OSUtils.isWindows()) {
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS)));
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP)));
        } 
        else {
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.HOME)));
        }
        
        return dirs;
    }
    
    /**
     * Determines the OS specific list of directories to exclude
     */
    public static  Collection<File> getExcludedDirectories() {
        Collection<File> dirs = new LinkedList<File>();
        
        if (OSUtils.isAnyMac()) {
            dirs.add(new File(SystemUtils.getSpecialPath(SpecialLocations.APPLICATION_DATA)));
        }
        
        return dirs;
    }
}
