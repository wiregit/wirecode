package com.limegroup.gnutella.version;


class Version implements Comparable {
    
    private final String v;
    private final int major;
    private final int minor;
    private final int service;
    
    /**
     * Constructs a new Version.
     */
    Version(String s) throws VersionFormatException {
        v = s;

        int[] nums = parse(s);
        major = nums[0];
        minor = nums[1];
        service = nums[2];
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
	    int major, minor, service;
	    int dot1, dot2;

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
                
            service = Integer.parseInt(vers.substring(dot2 + 1, q));
        } catch(NumberFormatException nfe) {
            throw new VersionFormatException(vers);
        }
        
        return new int[] { major, minor, service };
    }
    
}