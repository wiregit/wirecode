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
     * Modifies the loginwindow.plist file to either include or exclude
     * starting up LimeWire.
     */
    public static void modifyLoginWindowPList(boolean allow) {
        NSUserDefaults defaults = NSUserDefaults.standardUserDefaults();
        NSDictionary logins = defaults.persistentDomainForName("loginwindow");
        if(logins == null) {
            logins = new NSMutableDictionary();
            ((NSMutableDictionary)logins).setObjectForKey(new NSMutableArray(),
                                        "AutoLaunchedApplicationDictionary");
        }
        Object temp; // Used for the assignment when retrieiving as an object.
        
        // Retrieve the array that contains the various dictionaries
        temp = logins.objectForKey("AutoLaunchedApplicationDictionary");
        if(temp == null)
            temp = new NSMutableArray();
        // if it is not an NSMutableArray, exit (we dunno how to recover)
        if(!(temp instanceof NSMutableArray))
            return;
            
        NSMutableArray allApps = (NSMutableArray)temp;
        
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
            NSMutableDictionary itemInfo = (NSMutableDictionary)temp;
            
            Object path = itemInfo.objectForKey("Path");
            // If it isn't a String, ignore it because we
            // won't know how to handle it.
            if(!(path instanceof String))
                continue;
            String itemPath = (String)path;
            if(itemPath.indexOf("LimeWire.app") != -1) {
                found = true;
                if(!allow)
                    allApps.removeIdenticalObject(itemInfo);
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
            return "/Applications/LimeWire/LimeWire.app/";
        } else {
            String appDir = CommonUtils.getCurrentDirectory().getPath();
            int app = appDir.indexOf("LimeWire.app");
            appDir = appDir.substring(0, app + "LimeWire.app".length() + 1);
            return appDir;
        }
    }
}