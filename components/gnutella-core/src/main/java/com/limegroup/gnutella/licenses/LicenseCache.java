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
    
    private final Map /* String (URL) -> License */ licenses = createMap();

    private static final LicenseCache INSTANCE = new LicenseCache();
    private LicenseCache() {}
    public static LicenseCache instance() { return INSTANCE; }
    
    /**
     * Adds a verified license.
     */
    synchronized void addVerifiedLicense(License license) {
        String url = license.getLicenseURI().toString();
        licenses.put(url, license);
    }
    
    /**
     * Retrieves the cached license for the specified URI, substituting
     * the license string for a new one.
     */
    synchronized License getLicense(String licenseString, URI licenseURI) {
        License license = (License)licenses.get(licenseURI.toString());
        if(license != null)
            return license.copy(licenseString);
        else
             return null;
    }
    
    /**
     * Determines if the license is verified for the given URN and URI.
     */
    synchronized boolean isVerifiedAndValid(URN urn, URI uri) {
        License license = (License)licenses.get(uri.toString());
        if(license != null) {
            if(!license.isValid(urn))
                return false;
            URN expect = license.getExpectedURN();
            if(expect != null)
                return expect.equals(urn);
            else // cannot do URN match if no expected URN.
                return true;
        } else {
            return false; // unverified.
        }
    }
    
   /**
     * Loads values from cache file, if available
     */
    private Map createMap() {
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
                    if( !(key instanceof String) || !(value instanceof License) )
                        i.remove();
                }
            }
            return map;
        } catch(Throwable t) {
            LOG.error("Can't read licenses", t);
            return new HashMap();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // all we can do is try to close it
                }
            }
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
            if(license.getLastVerifiedTime() < cutoff)
                i.remove();
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        removeOldEntries();

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(CACHE_FILE)));
            oos.writeObject(licenses);
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
    }
}