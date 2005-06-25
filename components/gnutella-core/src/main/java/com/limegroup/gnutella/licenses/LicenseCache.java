package com.limegroup.gnutella.licenses;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.httpclient.URI;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * A repository of licenses.
 */
class LicenseCache {
    
    private static final Log LOG = LogFactory.getLog(LicenseCache.class);
    
    /**
     * The amount of time to keep a license in the cache.
     */
    private static final long EXPIRY_TIME = 7 * 24 * 60 * 60 * 1000; // one week.    
    
    /**
     * File where the licenses are serialized.
     */
    private final File CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "licenses.cache");        
    
    /**
     * A map of Licenses.  One License per URI.
     */
    private Map /* URI -> License */ licenses;
    
    /**
     * An extra map of data that Licenses can use
     * to cache info.  This information lasts forever.
     */
    private Map /* Object -> Object */ data;
    
    /**
     * Whether or not data is dirty since the last time we wrote to disk.
     */
    private boolean dirty = false;

    private static final LicenseCache INSTANCE = new LicenseCache();
    private LicenseCache() { deserialize(); }
    public static LicenseCache instance() { return INSTANCE; }
    
    /**
     * Adds a verified license.
     */
    synchronized void addVerifiedLicense(License license) {
        licenses.put(license.getLicenseURI(), license);
        dirty = true;
    }
    
    /**
     * Adds data.
     */
    synchronized void addData(Object key, Object value) {
        data.put(key, value);
        dirty = true;
    }
    
    /**
     * Retrieves the cached license for the specified URI, substituting
     * the license string for a new one.
     */
    synchronized License getLicense(String licenseString, URI licenseURI) {
        License license = (License)licenses.get(licenseURI);
        if(license != null)
            return license.copy(licenseString, licenseURI);
        else
             return null;
    }
    
    /**
     * Gets details.
     */
    synchronized Object getData(Object key) {
        return data.get(key);
    } 
    
    /**
     * Determines if the license is verified for the given URN and URI.
     */
    synchronized boolean isVerifiedAndValid(URN urn, URI uri) {
        License license = (License)licenses.get(uri);
        return license != null && license.isValid(urn);
    }
    
   /**
     * Loads values from cache file, if available
     */
    private void deserialize() {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(CACHE_FILE)));
            Map map = (Map)ois.readObject();
            if(map != null) {
                for(Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                    // Remove values that aren't correct.
                    Map.Entry next = (Map.Entry)i.next();
                    Object key = next.getKey();
                    Object value = next.getValue();
                    if( !(key instanceof URI) || !(value instanceof License) ) {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Invalid k[" + key + "], v[" + value + "]");
                        i.remove();
                    }
                }
            } else 
            	map = new HashMap();
            
            licenses = map;
            
            data = (Map)ois.readObject();
            removeOldEntries();
        } catch(Throwable t) {
            LOG.error("Can't read licenses", t);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // all we can do is try to close it
                }
            }
            
            if(licenses == null)
                licenses = new HashMap();
            if(data == null)
                data = new HashMap();
        }
    }
    
   /**
     * Removes any stale entries from the map so that they will automatically
     * be replaced.
     */
    private void removeOldEntries() {
        long cutoff = System.currentTimeMillis() - EXPIRY_TIME;
        
        // discard outdated info
        for(Iterator i = licenses.values().iterator(); i.hasNext(); ) {
            License license = (License)i.next();
            if(license.getLastVerifiedTime() < cutoff) {
                dirty = true;
                i.remove();
            }
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        if(!dirty)
            return;
        
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(CACHE_FILE)));
            oos.writeObject(licenses);
            oos.writeObject(data);
            oos.flush();
        } catch (IOException e) {
            ErrorService.error(e);
        } finally {
            if(oos != null) {
                try {
                    oos.close();
                } catch(IOException ignored) {}
            }
        }
        
        dirty = false;
    }
}