package org.limewire.ui.swing.util;

import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

/**
 * A collection of utility methods for OSX.
 * These methods should only be called if run from OSX,
 * otherwise ClassNotFoundErrors may occur.
 * 
 * Clients may use the method isNativeLibraryLoadedCorrectly() 
 * to check whether the native library loaded correctly.
 * If not, they may choose to disable certain user interface
 * features to reflect this state.
 * 
 * <p>
 * To determine if the Cocoa Foundation classes are present,
 * use the method CommonUtils.isCocoaFoundationAvailable().
 */
public class MacOSXUtils {
    
    /**
     * The application bundle identifier for the LimeWire application that is packed into its Info.plist config file.
     */
    public static final String LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER = "com.limegroup.gnutella";
    
    /**
     * The name of the app that launches.
     */
    private static final String APP_NAME = "LimeWire.app";

    private static boolean nativeLibraryLoadedCorrectly = false;
    
    static {
        if (OSUtils.isMacOSX()) {
            try {
                System.loadLibrary("MacOSXUtils");
                nativeLibraryLoadedCorrectly = true;
            } catch (UnsatisfiedLinkError err) {
                ErrorService.error(err);
            }
        }
    }
    
    /**
     * This returns a boolean indicating whether an exception occurred when loading the native library.
     * @return true if the native library loaded without any errors.
     */
    public static boolean isNativeLibraryLoadedCorrectly() {
        return nativeLibraryLoadedCorrectly;
    }

    private MacOSXUtils() {}
    
    /**
     * Modifies the loginwindow.plist file to either include or exclude
     * starting up LimeWire.
     */
    public static void setLoginStatus(boolean allow) {
        try {
            SetLoginStatusNative(allow);
        } catch(UnsatisfiedLinkError ule) {
            // Ignore, no big deal.
        }
    }
    
    /**
     * Gets the full user's name.
     */
    public static String getUserName() {
        try {
            return GetCurrentFullUserName();
        } catch(UnsatisfiedLinkError ule) {
            // No big deal, just return user name.
            return CommonUtils.getUserName();
        }
    }
    
    /**
     * Retrieves the app directory & name.
     * If the user is not running from the bundled app as we named it,
     * defaults to /Applications/LimeWire/ as the directory of the app.
     */
    public static String getAppDir() {
        String appDir = "/Applications/LimeWire/";
        String path = CommonUtils.getCurrentDirectory().getPath();
        int app = path.indexOf("LimeWire.app");
        if(app != -1)
            appDir = path.substring(0, app);
        return appDir + APP_NAME;
    }

    /**
     * This sets LimeWire as the default handler for this file type.
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     */
    public static void setLimewireAsDefaultFileTypeHandler(String fileType) {
        try {
            SetDefaultFileTypeHandler(fileType, LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER);
        } catch(UnsatisfiedLinkError ule) {
            // Ignore, no big deal.
        }
    }
    
    /**
     * This checks whether LimeWire is the default handler for this file type.
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     * @return true if LimeWire is the default handler for this file type
     */
    public static boolean isLimewireDefaultFileTypeHandler(String fileType) {
        try {
            return IsApplicationTheDefaultFileTypeHandler(fileType, LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER);
        } catch(UnsatisfiedLinkError ule) {
            // Ignore, no big deal.
        }
        
        return true;
    }

    /**
     * This checks whether any applications are registered as handlers for this fileType in the OS-X
     * launch services database.
     * 
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     * @return true if any application is registered as a handler for this file type
     */
    public static boolean isFileTypeHandled(String fileType) {
        try {
            return IsFileTypeHandled(fileType);
        } catch(UnsatisfiedLinkError ule) {
            // Ignore, no big deal.
        }
        
        return true;
    }
    
    /**
     * This tries to change the file type handler for the given file type from LimeWire to another application.
     * Basically, it just changes the default handler application to the first application in the list
     * returned by launch services that isn't LimeWire. It might fail if no other handlers are registered for this file type.  
     * The list of handlers that are used internally in this method should not be shown to users as they are probably not understandable
     * by users.  For example LimeWire is represented by the application bundle identifier com.limegroup.gnutella.
     * 
     * @param fileType -- the file extension for the file. this will be used to look up the file type's UTI (universal type identifier)
     */
    public static void tryChangingDefaultFileTypeHandler(String fileType) {
        try {
            String[] handlers = GetAllHandlersForFileType(fileType);
            if (handlers != null) {
                for (String handler : handlers) {
                    if (!handler.equals(LIMEWIRE_APPLICATION_BUNDLE_IDENTIFIER)) {
                        SetDefaultFileTypeHandler(fileType, handler);
                        break;
                    }
                }
            }
        } catch(UnsatisfiedLinkError ule) {
            // Ignore, no big deal.
        }
    }
    
    /**
     * Uses OS-X's launch services API to check whether any application has registered itself
     * as a handler for this application. 
     */
    private static final native boolean IsFileTypeHandled(String fileType);

    /**
     * Uses OS-X's launch services API to check whether the given application is the default handler for this
     * file type.
     */
    private static final native boolean IsApplicationTheDefaultFileTypeHandler(String fileType, String applicationBundleIdentifier);

    /**
     * Uses OS-X's launch services API to set the given application as the default handler for this file type.
     */
    private static final native int SetDefaultFileTypeHandler(String fileType, String applicationBundleIdentifier);

    /**
     * Open a native file dialog for selecting files and folders.
     * Native dialogs have the advantage of preserving the look and feel of
     * the platform while still allowing for multiple file selections.
     */
    private static final native String[] GetAllHandlersForFileType(String fileType); 

    /**
     * Gets the full user's name.
     */
    private static final native String GetCurrentFullUserName();
    
    /**
     * [Un]registers LimeWire from the startup items list.
     */
    private static final native void SetLoginStatusNative(boolean allow);
}