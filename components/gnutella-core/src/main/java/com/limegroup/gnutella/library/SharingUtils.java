package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.FileDesc;

public class SharingUtils {
    
    // TODO: refactor back into filemanger for test mocking... use instances.
    
    /** Subdirectory that is always shared */
    public static final File PROGRAM_SHARE;

    /** Subdirectory that also is always shared. */
    public static final File PREFERENCE_SHARE;

    /** Subdirectory used to share special application files */
    public static final File APPLICATION_SPECIAL_SHARE;
    
    


    static {
        File forceShare = new File(".", ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PROGRAM_SHARE = forceShare;
        
        forceShare = 
            new File(CommonUtils.getUserSettingsDir(), ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PREFERENCE_SHARE = forceShare;
        
        forceShare = 
            new File(CommonUtils.getUserSettingsDir(), ".AppSpecialShare").getAbsoluteFile();
        forceShare.mkdir();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        APPLICATION_SPECIAL_SHARE = forceShare;
    }
    
    /**
     * Returns true if this file is not too large, not too small,
     * not null, is a directory, can be read, is not hidden.  
     * Returns false otherwise.
     */
    public static boolean isFilePhysicallyShareable(File file) {
        if (file == null || !file.exists() || file.isDirectory() || !file.canRead() || file.isHidden() ) 
            return false;
                
        long fileLength = file.length();
        if (fileLength <= 0 || fileLength > MAX_FILE_SIZE) 
            return false;
        
        return true;
    }
    
    
    /**
     * Returns true iff <tt>file</tt> is a sensitive directory.
     */
    public static boolean isSensitiveDirectory(File folder) {
        if (folder == null)
            return false;

        String userHome = System.getProperty("user.home");
        if (folder.equals(new File(userHome)))
            return true;
        
        String userHomeShortDir = userHome.substring(userHome.lastIndexOf(File.separator)+1);

        String[] sensitive;
        if (OSUtils.isWindowsVista()) {
            //Windows Vista does not have the "My" prefix for the "My Documents" folder.  It is simply "Documents"
            // TODO: Are Local Settings & everything after that right for Vista?
            sensitive = new String[] { "Documents and Settings",
                    userHomeShortDir + File.separator + "Documents", "Desktop", "Program Files",
                    "Windows", "WINNT", "Users", "Local Settings", "Application Data", "Temp",
                    "Temporary Internet Files" };
        } else if (OSUtils.isWindows()) {
            sensitive = new String[] { "Documents and Settings", "My Documents", "Desktop",
                    "Program Files", "Windows", "WINNT", "Users", "Local Settings",
                    "Application Data", "Temp", "Temporary Internet Files" };
        } else if (OSUtils.isMacOSX()) {
            sensitive = new String[] { "Users", "System", "System Folder", "Previous Systems",
                    "private", "Volumes", "Desktop", "Applications", "Applications (Mac OS 9)",
                    "Network" };
        } else if (OSUtils.isPOSIX()) {
            sensitive = new String[] { "bin", "boot", "dev", "etc", "home", "mnt", "opt", "proc",
                    "root", "sbin", "usr", "var" };
        } else {
            sensitive = new String[0];
        }

        String folderPath = folder.getPath();
        for (String name : sensitive) {
            if (folderPath.endsWith(File.separator + name)){
                return true;
            }
        }

        return false;
    }
    
    /**
     * Determines if this FileDesc is a network share.
     */
    public static boolean isForcedShare(FileDesc desc) {
        return isForcedShare(desc.getFile());
    }
    
    /**
     * Determines if this File is a network share.
     */
    public static boolean isForcedShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isForcedShareDirectory(parent);
    }
    
    /**
     * Determines if this File is an application special share.
     */
    public static boolean isApplicationSpecialShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isApplicationSpecialShareDirectory(parent);
    }
    
    /**
     * Determines if this File is a network shared directory.
     */
    public static boolean isForcedShareDirectory(File f) {
        return f.equals(SharingUtils.PROGRAM_SHARE) || f.equals(SharingUtils.PREFERENCE_SHARE);
    }
    
    public static boolean isApplicationSpecialShareDirectory(File directory) {
        return directory.equals(SharingUtils.APPLICATION_SPECIAL_SHARE);
    }

    /**
     * Returns true if this folder should never be a shared or saved folder.
     */
    public static boolean isFolderBanned(File folder) {        
        //  check for system roots
        File[] faRoots = File.listRoots();
        if (faRoots != null && faRoots.length > 0) {
            for (int i = 0; i < faRoots.length; i++) {
                if (folder.equals(faRoots[i]))
                    return true;
            }
        }
        
        // Check for the folder name being 'Cookies', or 'Cookies\Low' [vista]
        // TODO: Make sure this is i18n-safe
        String name = folder.getName().toLowerCase(Locale.US);
        if(name.equals("cookies"))
            return true;
        else if(name.equals("low")) {
            String parent = folder.getParent();
            if(parent != null && parent.toLowerCase(Locale.US).equals("cookies"))
                return true;
        }
        
        return false;
    }
    
}
