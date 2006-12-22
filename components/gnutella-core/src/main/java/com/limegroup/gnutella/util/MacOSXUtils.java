package com.limegroup.gnutella.util;

import java.util.Enumeration;

import com.apple.cocoa.foundation.NSMutableArray;
import com.apple.cocoa.foundation.NSMutableDictionary;
import com.apple.cocoa.foundation.NSObject;
import com.apple.cocoa.foundation.NSSystem;
import com.apple.cocoa.foundation.NSUserDefaults;

/**
 * A collection of utility methods for OSX.
 * These methods should only be called if run from OSX,
 * otherwise ClassNotFoundErrors may occur.
 *
 * To determine if the Cocoa Foundation classes are present,
 * use the method CommonUtils.isCocoaFoundationAvailable().
 */
public class MacOSXUtils {
    private MacOSXUtils() {}
    
    /**
     * The login domain.
     */
    private static final String LOGIN_DOMAIN = "loginwindow";
    
    /**
     * The name of the dictionary which contains the apps that are launched.
     */
    private static final String DICTIONARY =
        "AutoLaunchedApplicationDictionary";
    
    /**
     * The key for "hide"
     */
    private static final String HIDE = "Hide";
    
    /**
     * The key for "path";
     */
    private static final String PATH = "Path";
    
    /**
     * The name of the app that launches.
     */
    private static final String APP_NAME = "LimeWire At Login.app";

    /**
     * Creates a mutable clone of the specified object.
     */
    private static Object createMutableClone(Object a)
      throws CloneNotSupportedException {
        if(a == null)
            throw new CloneNotSupportedException();
        else if (!(a instanceof NSObject))
            throw new CloneNotSupportedException();
        else
            return ((NSObject)a).mutableClone();
    }

    /**
     * Modifies the loginwindow.plist file to either include or exclude
     * starting up LimeWire.
     */
    public static void setLoginStatus(boolean allow) {
        Object temp; // Used for the assignment when retrieiving as an object.
        NSUserDefaults defaults = NSUserDefaults.standardUserDefaults();

        temp = defaults.persistentDomainForName(LOGIN_DOMAIN);
        // If no domain existed, create one and make its initial dictionary.
        if(temp == null) {
            temp = new NSMutableDictionary();
            ((NSMutableDictionary)temp).setObjectForKey(new NSMutableArray(),
                                        DICTIONARY);
        } else if(!(temp instanceof NSMutableDictionary))
            return; // nothing we can do.

        NSMutableDictionary logins = null;
        try {
            logins = (NSMutableDictionary)createMutableClone(temp);
        } catch(CloneNotSupportedException cnso) {
            //nothing we can do, exit.
            return;
        }
        
        // Retrieve the array that contains the various dictionaries
        temp = logins.objectForKey(DICTIONARY);
        // If no object existed, create one.
        if(temp == null)
            temp = new NSMutableArray();
        // if it is not an NSMutableArray, exit (we dunno how to recover)
        else if(!(temp instanceof NSMutableArray))
            return;

        NSMutableArray allApps = null;
        try {
            allApps = (NSMutableArray)createMutableClone(temp);
        } catch(CloneNotSupportedException cnso) {
            // nothing we can do, exit.
            return;
        }
        
        Enumeration items = allApps.objectEnumerator();
        boolean found = false;
        // Iterate through each item in the array,
        // looking for one whose 'Path' key has a value
        // containing the stub startup app LimeWire uses.
        while(items.hasMoreElements()) {
            temp = items.nextElement();
            // If it isn't an NSMutableDictionary, ignore it
            // because we won't know how to handle it.
            if(!(temp instanceof NSMutableDictionary))
                continue;

            NSMutableDictionary itemInfo = null;
            try {
                itemInfo = (NSMutableDictionary)createMutableClone(temp);
            } catch(CloneNotSupportedException cnse) {
                continue; // can't do anything, continue.
            }
            
            Object path = itemInfo.objectForKey(PATH);
            // If it isn't a String, ignore it because we
            // won't know how to handle it.
            if(!(path instanceof String))
                continue;
            String itemPath = (String)path;
            if(itemPath.indexOf(APP_NAME) != -1) {
                found = true;
                // If not allowed, remove.
                // We must remove with temp, not itemInfo, because itemInfo
                // is a clone.
                if(!allow)
                    allApps.removeObject(temp);
                else
                    itemInfo.setObjectForKey(getAppDir(), PATH);
                break;
            }
        }
        
        if(!found) {
            if(!allow)
                return;
            else {
                NSMutableDictionary newItem = new NSMutableDictionary();
                newItem.setObjectForKey(new Integer(0), HIDE);
                newItem.setObjectForKey(getAppDir(), PATH);
                allApps.addObject(newItem);
            }
        }
        
        //Make sure we set add our new allApps to the dictionary.
        logins.setObjectForKey(allApps, DICTIONARY);
        
        // Set the new domain.
        defaults.setPersistentDomainForName(logins, LOGIN_DOMAIN);
        // Synchronize it to disk immediately
        defaults.synchronize();
    }
    
    /**
     * Gets the full user's name.
     */
    public static String getUserName() {
        return NSSystem.currentFullUserName();
    }
    
    /**
     * Retrieves the app directory & name.
     * If the user is not running from the bundled app as we named it,
     * defaults to /Applications/LimeWire/ as the directory of the app.
     */
    private static String getAppDir() {
        String appDir = "/Applications/LimeWire/";
        String path = LimeWireUtils.getCurrentDirectory().getPath();
        int app = path.indexOf("LimeWire.app");
        if(app != -1)
            appDir = path.substring(0, app);
        return appDir + APP_NAME;
    }
}