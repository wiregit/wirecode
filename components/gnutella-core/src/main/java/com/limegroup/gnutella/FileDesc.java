/**
 * This class is a wrapper for file information.
 *
 * Modified by Sumeet Thadani (5/21): No need to store meta-data here
 * Modified by Gordon Mohr (2002/02/19): Added URN storage, calculation, caching
 */

package com.limegroup.gnutella;

import com.bitzi.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.security.*;
import java.util.Enumeration;

public class FileDesc {
    static public Hashtable /* String -> String */ UrnCache;
    
    public int _index;
    public String _path;
    public String _name;
    public int _size;
    public long _modTime;
    public HashSet /* of Strings */ _urns; // one or more "urn:" names for this file
		
    /**
     * @param i index of the file
     * @param n the name of the file (e.g., "funny.txt")
     * @param p the fully-qualified path of the file
     *  (e.g., "/home/local/funny.txt")
     * @param s the size of the file, in bytes.  (Note that
     *  files are currently limited to Integer.MAX_VALUE bytes
     *  length, i.e., 2048MB.)
     */
    public FileDesc(int i, String n, String p, int s) {
        _index = i;
        _name = n;
        _path = p;
        _size = s;
        fillUrnsFromCache();
        // if(shouldCalculateUrns()) calculateUrns();
    }

    public void print() {
        System.out.println("Name: " + _name);
        System.out.println("Index: " + _index);
        System.out.println("Size: " + _size);
        System.out.println("Path: " + _path);
        System.out.println("URNs: " + _urns);
        System.out.println(" ");
    }
    
    //
    // Query handling
    
    public Response responseFor(QueryRequest qr) {
        Response r = new Response(_index,_size,_name);
        if(_urns==null) return r;
        
        /** Popular approach: return all URNs **/
        Iterator allUrns = _urns.iterator();
        while(allUrns.hasNext()) {
            r.addUrn((String)allUrns.next());
        }
        
        /** 
         * Technically proper approach (by HUGE v.0.93): 
         * give only URNs that are requested
         *
        if(qr.getRequestedUrnTypes()==null) return r;
        Iterator outer = qr.getRequestedUrnTypes().iterator();
        while (outer.hasNext()) {
            Iterator inner = _urns.iterator();
            String req = (String)outer.next();
            while(inner.hasNext()) {
                String urn = (String)inner.next();
                if(urn.startsWith(req)) {
                    r.addUrn(urn);
                }
            }
        }
        /**/
        return r;
        
    }
    
    //
    //
    // URN-calculation and handling
    //
    
    /**
     * adds any URNs remembered from a previous session;
     */
    public void fillUrnsFromCache() {
        if (UrnCache==null) {
            FileDesc.initCache();
        }
        _modTime = (new File(_path)).lastModified();
        if (_modTime==0L) return; // don't trust failed mod times
        
        HashSet cachedUrns = (HashSet)UrnCache.get(_modTime+" "+_path);
        if(cachedUrns!=null) {
            if(_urns==null) _urns = new HashSet();
            Iterator iter = cachedUrns.iterator();
            while(iter.hasNext()){
                String urn = (String)iter.next();
                _urns.add(urn);
            }
        } // else just leave _urns empty for now
    }
    
    /**
     * would calling the calculation method add useful URNs?
     */
    public boolean shouldCalculateUrns() {
        if (_urns==null) return true; 
        Iterator iter = _urns.iterator();
        while(iter.hasNext()) {
            if(((String)iter.next()).startsWith("urn:sha1:")) {
                return false; // we already have all the values we can calculate
            }
        }
        return true; // we could calculate a SHA1
    }
    
    /**
     * adds any URNs that can be locally calculated; may take a while to complete on large files
     */
    public void calculateUrns() {
        try {
            // update modTime
            File f = new File(_path);
            _modTime = f.lastModified();
            
            FileInputStream fis = new FileInputStream(_path);   
            // we can only calculate SHA1 for now
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] buffer = new byte[16384];
            int read;
            while ((read=fis.read(buffer))!=-1) {
                md.update(buffer,0,read);
            }
            fis.close();
            byte[] sha1 = md.digest();
            if(_urns==null) _urns = new HashSet();
            // preferred casing: lowercase "urn:sha1:", uppercase encoded value
            // note that all URNs are case-insensitive for the "urn:<type>:" part,
            // but some MAY be case-sensitive thereafter (SHA1/Base32 is case insensitive)
            _urns.add("urn:sha1:"+Base32.encode(sha1));
            persistUrns();
        } catch (IOException e) {
            // relatively harmless to not have URN
        } catch (NoSuchAlgorithmException e) {
            // relatively harmless to not have URN    
        }
    }
    
    /**
     * Verify that given URN applies to file
     */
    public boolean satisfiesUrn(String urn) {
        // first check if modified since last hashing
        if ((new File(_path)).lastModified()!=_modTime) {
            // recently modified; throw out SHA1 values
            Iterator iter = _urns.iterator();
            while(iter.hasNext()){
                if (((String)iter.next()).startsWith("urn:sha1:")) {
                    iter.remove();
                }
            }
        }
        // now check if given urn matches
        Iterator iter = _urns.iterator();
        while(iter.hasNext()){
            if (urn.equals((String)iter.next())) {
                return true;
            }
        }
        // no match
        return false;
    }
    
    /**
     * Return SHA1 URN, if available
     */
    public String getSHA1() {
        if (_urns==null) return null;
        Iterator iter = _urns.iterator(); 
        while(iter.hasNext()) {
            String urn = (String)iter.next();
            if(urn.startsWith("urn:sha1:")) {
                return urn;
            }
        }
        return null;
    }


    /**
     * remember URNs in UrnCache
     */
    private void persistUrns() {
        if (_urns==null) return;
        UrnCache.put(_modTime+" "+_path,_urns);
    }
    
    //
    // UrnCache Management
    //
    
    /**
     * load values from cache file, if available
     */
    static private void initCache() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("fileurns.cache"));
            UrnCache = (Hashtable)ois.readObject();
            ois.close();
        } catch (Exception e) {
            // lack of cache is non-fatal
        } 
        if (UrnCache == null) {
            UrnCache = new Hashtable();
            return;
        }
        // discard outdated info
        Iterator iter = UrnCache.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            long modTime=Long.parseLong(key.substring(0,key.indexOf(' ')));
            String path=key.substring(key.indexOf(' ')+1);
            // check to see if file still exists unmodified
            File f = new File(path);
            if (!f.exists()||f.lastModified()!=modTime) {
                iter.remove();
            }
        }
    }
    
    /**
     * write cache to disk to save recalc time later
     */
    static public void persistCache() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("fileurns.cache"));
            oos.writeObject(UrnCache);
            oos.close();
        } catch (Exception e) {
            // no great loss
        }
    }
}


