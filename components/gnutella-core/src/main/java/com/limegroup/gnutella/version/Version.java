package com.limegroup.gnutella.version;


class Version implements Comparable {
    
    private final String v;
    private final int major;
    private final int minor;
    private final int service;
    private final int revision;
    
    /**
     * Constructs a new Version.
     */
    Version(String s) throws VersionFormatException {
        v = s;

        int[] nums = parse(s);
        major = nums[0];
        minor = nums[1];
        service = nums[2];
        revision = nums[3];
    }
    
    /**
     * Returns the version.
     */
    public String getVersion() {
        return v;
    }
    
    /**
     * Returns the version.
     */
    public String toString() {
        return getVersion();
    }
    
    /**
     * Compares two versions.
     */
    public int compareTo(Object o) {
        Version other = (Version)o;
        if(major == other.major)
            if(minor == other.minor)
                if(service == other.service)
                    // if revision == other.revision
                        // return 0;
                    // else
                        return revision - other.revision;
                else
                    return service - other.service;
            else
                return minor - other.minor;
        else
            return major - other.major;
    }
    
    /**
     * Parses a version for major/minor/service.
     */
    private int[] parse(String vers) throws VersionFormatException {
	    int major, minor, service, revision;
	    int dot1, dot2, lastNum;

        dot1 = vers.indexOf(".");
	    if(dot1 == -1)
	        throw new VersionFormatException(vers);
	    dot2 = vers.indexOf(".", dot1 + 1);
	    if(dot2 == -1)
	        throw new VersionFormatException(vers);
	        
        try {
            major = Integer.parseInt(vers.substring(0, dot1));
        } catch(NumberFormatException nfe) {
            throw new VersionFormatException(vers);
        }
        
        try {
            minor = Integer.parseInt(vers.substring(dot1 + 1, dot2));
        } catch(NumberFormatException nfe) {
            throw new VersionFormatException(vers);
        }
        
        try {
            int q = dot2 + 1;
            while(q < vers.length() &&  Character.isDigit(vers.charAt(q)))
                q++;
            
            lastNum = q;    
            service = Integer.parseInt(vers.substring(dot2 + 1, q));
        } catch(NumberFormatException nfe) {
            throw new VersionFormatException(vers);
        }
        
        revision = 0;
        try {
            int q = lastNum + 1;
            while (q < vers.length() && !Character.isDigit(vers.charAt(q)))
                q++;
                
            if(q < vers.length())
                revision = Integer.parseInt(vers.substring(q));
        } catch(NumberFormatException okay) {
            // not everything will have a revision digit.
        }
            
        
        return new int[] { major, minor, service, revision };
    }
    
}