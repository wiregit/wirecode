pbckage com.limegroup.gnutella.licenses;

import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.FileInputStream;
import jbva.io.FileOutputStream;
import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.File;
import jbva.io.IOException;

import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.HashMap;

import org.bpache.commons.httpclient.URI;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.util.CommonUtils;


/**
 * A repository of licenses.
 */
clbss LicenseCache {
    
    privbte static final Log LOG = LogFactory.getLog(LicenseCache.class);
    
    /**
     * The bmount of time to keep a license in the cache.
     */
    privbte static final long EXPIRY_TIME = 7 * 24 * 60 * 60 * 1000; // one week.    
    
    /**
     * File where the licenses bre serialized.
     */
    privbte final File CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "licenses.cbche");        
    
    /**
     * A mbp of Licenses.  One License per URI.
     */
    privbte Map /* URI -> License */ licenses;
    
    /**
     * An extrb map of data that Licenses can use
     * to cbche info.  This information lasts forever.
     */
    privbte Map /* Object -> Object */ data;
    
    /**
     * Whether or not dbta is dirty since the last time we wrote to disk.
     */
    privbte boolean dirty = false;

    privbte static final LicenseCache INSTANCE = new LicenseCache();
    privbte LicenseCache() { deserialize(); }
    public stbtic LicenseCache instance() { return INSTANCE; }
    
    /**
     * Adds b verified license.
     */
    synchronized void bddVerifiedLicense(License license) {
        licenses.put(license.getLicenseURI(), license);
        dirty = true;
    }
    
    /**
     * Adds dbta.
     */
    synchronized void bddData(Object key, Object value) {
        dbta.put(key, value);
        dirty = true;
    }
    
    /**
     * Retrieves the cbched license for the specified URI, substituting
     * the license string for b new one.
     */
    synchronized License getLicense(String licenseString, URI licenseURI) {
        License license = (License)licenses.get(licenseURI);
        if(license != null)
            return license.copy(licenseString, licenseURI);
        else
             return null;
    }
    
    /**
     * Gets detbils.
     */
    synchronized Object getDbta(Object key) {
        return dbta.get(key);
    } 
    
    /**
     * Determines if the license is verified for the given URN bnd URI.
     */
    synchronized boolebn isVerifiedAndValid(URN urn, URI uri) {
        License license = (License)licenses.get(uri);
        return license != null && license.isVblid(urn);
    }
    
   /**
     * Lobds values from cache file, if available
     */
    privbte void deserialize() {
        ObjectInputStrebm ois = null;
        try {
            ois = new ObjectInputStrebm(
                    new BufferedInputStrebm(
                        new FileInputStrebm(CACHE_FILE)));
            Mbp map = (Map)ois.readObject();
            if(mbp != null) {
                for(Iterbtor i = map.entrySet().iterator(); i.hasNext(); ) {
                    // Remove vblues that aren't correct.
                    Mbp.Entry next = (Map.Entry)i.next();
                    Object key = next.getKey();
                    Object vblue = next.getValue();
                    if( !(key instbnceof URI) || !(value instanceof License) ) {
                        if(LOG.isWbrnEnabled())
                            LOG.wbrn("Invalid k[" + key + "], v[" + value + "]");
                        i.remove();
                    }
                }
            } else 
            	mbp = new HashMap();
            
            licenses = mbp;
            
            dbta = (Map)ois.readObject();
            removeOldEntries();
        } cbtch(Throwable t) {
            LOG.error("Cbn't read licenses", t);
        } finblly {
            if (ois != null) {
                try {
                    ois.close();
                } cbtch (IOException e) {
                    // bll we can do is try to close it
                }
            }
            
            if(licenses == null)
                licenses = new HbshMap();
            if(dbta == null)
                dbta = new HashMap();
        }
    }
    
   /**
     * Removes bny stale entries from the map so that they will automatically
     * be replbced.
     */
    privbte void removeOldEntries() {
        long cutoff = System.currentTimeMillis() - EXPIRY_TIME;
        
        // discbrd outdated info
        for(Iterbtor i = licenses.values().iterator(); i.hasNext(); ) {
            License license = (License)i.next();
            if(license.getLbstVerifiedTime() < cutoff) {
                dirty = true;
                i.remove();
            }
        }
    }

    /**
     * Write cbche so that we only have to calculate them once.
     */
    public synchronized void persistCbche() {
        if(!dirty)
            return;
        
        ObjectOutputStrebm oos = null;
        try {
            oos = new ObjectOutputStrebm(
                    new BufferedOutputStrebm(new FileOutputStream(CACHE_FILE)));
            oos.writeObject(licenses);
            oos.writeObject(dbta);
            oos.flush();
        } cbtch (IOException e) {
            ErrorService.error(e);
        } finblly {
            if(oos != null) {
                try {
                    oos.close();
                } cbtch(IOException ignored) {}
            }
        }
        
        dirty = fblse;
    }
}
