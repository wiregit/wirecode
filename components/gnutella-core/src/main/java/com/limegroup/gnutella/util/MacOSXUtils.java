package com.limegroup.gnutella.util;

import com.apple.cocoa.foundation.*;
import java.util.Enumeration;

/**
 * A collection of utility methods for OSX.
 * These methods should only be called if run from OSX,
 * otherwise ClassNotFoundErrors may occur.
 */
public class MacOSXUtils {
    private MacOSXUtils() {}
    
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

        temp = defaults.persistentDomainForName("loginwindow");
        // If no domain existed, create one and make its initial dictionary.
        if(temp == null) {
            temp = new NSMutableDictionary();
            ((NSMutableDictionary)temp).setObjectForKey(new NSMutableArray(),
                                        "AutoLaunchedApplicationDictionary");
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
        temp = logins.objectForKey("AutoLaunchedApplicationDictionary");
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
        // containing LimeWire.app.
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
                cnse.printStackTrace();
                continue; // can't do anything, continue.
            }
            
            Object path = itemInfo.objectForKey("Path");
            // If it isn't a String, ignore it because we
            // won't know how to handle it.
            if(!(path instanceof String))
                continue;
            String itemPath = (String)path;
            if(itemPath.indexOf("/LimeWire/LoginStartup.app") != -1) {
                found = true;
                // If not allowed, remove.
                // We must remove with temp, not itemInfo, because itemInfo
                // is a clone.
                if(!allow)
                    allApps.removeIdenticalObject(temp);
                else
                    itemInfo.setObjectForKey(getAppDir(), "Path");
                break;
            }
        }
        
        if(!found) {
            if(!allow)
                return;
            else {
                NSMutableDictionary newItem = new NSMutableDictionary();
                newItem.setObjectForKey(new Integer(0), "Hide");
                newItem.setObjectForKey(getAppDir(), "Path");
                allApps.addObject(newItem);
            }
        }
        
        //Make sure we set add our new allApps to the dictionary.
        logins.setObjectForKey(allApps, "AutoLaunchedApplicationDictionary");
        
        // Set the new domain.
        defaults.setPersistentDomainForName(logins, "loginwindow");
        // Synchronize it to disk immediately
        defaults.synchronize();
    }
    
    /**
     * Retrieves the loginwindow path as a domain (suitable for the 'defaults'
     * command).
     */
    public static String getLoginWindow() {
        return CommonUtils.getUserHomeDir().getPath() + 
            "/Library/Preferences/loginwindow";
    }
    
    /**
     * Retrieves the app directory.
     * If the user is running off CVS (as distinguished by the version
     * being @version@), then a dummy directory of 
     * /Applications/LimeWire/LimeWire.app/ is returned.
     */
    public static String getAppDir() {
        if(CommonUtils.isTestingVersion()) {
            return "/Applications/LimeWire/LoginStartup.app/";
        } else {
            String appDir = CommonUtils.getCurrentDirectory().getPath();
            int app = appDir.indexOf("LimeWire.app");
            appDir = appDir.substring(0, app) + "LoginStartup.app/";
            return appDir;
        }
    }
}