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

public final class FileDesc {
    
	private final File _file;
    public final int _index;
    public final String _path;
    public final String _name;
    public final int _size;
    public long _modTime;
    public HashSet /* of Strings */ _urns; // one or more "urn:" names for this file
		
    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
	 *
	 * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
	 * @param urns the <tt>HashSet</tt> of URNs for this <tt>FileDesc</tt>
     */
    public FileDesc(File file, int index, HashSet urns) {
		
		_file = file;
        _index = index;
        _name = file.getName();
        _path = file.getAbsolutePath(); //TODO: right method?
        _size = (int)file.length();
        _modTime = file.lastModified();
        _urns = urns;
        //if(this.shouldCalculateUrns()) {
		//this.calculateUrns();
		//}
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
     * adds any URNs that can be locally calculated; may take a while to 
	 * complete on large files.
     */
    public void calculateUrns() {
        try {
            // update modTime
            _modTime = _file.lastModified();
            
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
            // but some MAY be case-sensitive thereafter (SHA1/Base32 is case 
			//insensitive)
            _urns.add("urn:sha1:"+Base32.encode(sha1));
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
        if (_file.lastModified()!=_modTime) {
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
	 * Returns the <tt>File</tt> instance for this <tt>FileDesc</tt>.
	 *
	 * @return the <tt>File</tt> instance for this <tt>FileDesc</tt>
	 */
	public File getFile() {
		return _file;
	}

    /**
     * Opens an input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instance could not be found
     */
    public InputStream getInputStream() throws FileNotFoundException {
		return new FileInputStream(_file);
    }
}


