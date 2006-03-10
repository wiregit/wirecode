package com.limegroup.gnutella.auth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;


/**
 * A repository of content responses.
 */
class ContentCache {
    
    private static final Log LOG = LogFactory.getLog(ContentCache.class);
    
    /** The amount of time to keep a response in the cache. */
    private static final long EXPIRY_TIME = 7 * 24 * 60 * 60 * 1000; // one week.    
    
    /** File where the licenses are serialized. */
    private final File CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "responses.cache");
    
    /** Map of SHA1 to Responses. */
    private Map /* URN -> ContentResponseData */ responses = new HashMap();    
    
    /** Whether or not data is dirty since the last time we wrote to disk. */
    private boolean dirty = false;
    
    /** Determines if there is a response for the given URN. */
    synchronized boolean hasResponseFor(URN urn) {
        return responses.containsKey(urn);
    }
    
    /** Adds the given response for the given URN. */
    synchronized void addResponse(URN urn, ContentResponseData response) {
        responses.put(urn, response);
        dirty = true;
    }
    
    /** Gets the response for the given URN. */
    synchronized ContentResponseData getResponse(URN urn) {
        return (ContentResponseData)responses.get(urn);
    }
    
    /** Initializes this cache. */
    synchronized void initialize() {
        dirty = false;        
        deserialize();
    }
    
    /** Writes to disk. */
    synchronized void writeToDisk() {
        if(dirty)
            persistCache();
        dirty = false;
    }
    
   /**
     * Loads values from cache file, if available.
     */
    private void deserialize() {
        long cutoff = System.currentTimeMillis() - EXPIRY_TIME;
        
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(CACHE_FILE)));
            Map map = (Map)ois.readObject();
            if(map != null) {
                for(Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                    // Remove values that aren't correct.
                    Map.Entry next = (Map.Entry)i.next();
                    Object key = next.getKey();
                    Object value = next.getValue();
                    if( !(key instanceof URN) || !(value instanceof ContentResponseData) ) {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Invalid k[" + key + "], v[" + value + "]");
                        i.remove();
                        dirty = true;
                    }
                    if(((ContentResponseData)value).getCreationTime() < cutoff) {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Removing old response [" + value + "]");
                        i.remove();
                        dirty = true;
                    }
                }
            } else {
                map = new HashMap();
            }
            
            responses = map;
        } catch(Throwable t) {
            dirty = true;
            LOG.error("Can't read responses", t);
        } finally {
            IOUtils.close(ois);
            if(responses == null)
                responses = new HashMap();
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public void persistCache() {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(CACHE_FILE)));
            oos.writeObject(responses);
            oos.flush();
        } catch (IOException e) {
            ErrorService.error(e);
        } finally {
            IOUtils.close(oos);
        }
    }
}