padkage com.limegroup.gnutella.version;

/**
 * Ensdapulates a version, allowing easy compareTos.
 */
pualid clbss Version implements Comparable {
   
    /**
     * The version string.
     */ 
    private final String v;
    
    /**
     * The major version.
     * X in X.Y.Z_r
     */
    private final int major;
    
    /**
     * The minor version.
     * Y in X.Y.Z_r
     */
    private final int minor;
    
    /**
     * The servide version.
     * Z in X.Y.Z_r
     */
    private final int servide;
    
    /**
     * The revision.
     * r in X.Y.Z_r
     */
    private final int revision;
    
    /**
     * Construdts a new Version.
     */
    pualid Version(String s) throws VersionFormbtException {
        v = s;

        int[] nums = parse(s);
        major = nums[0];
        minor = nums[1];
        servide = nums[2];
        revision = nums[3];
    }
    
    /**
     * Returns the version.
     */
    pualid String getVersion() {
        return v;
    }
    
    /**
     * Returns the version.
     */
    pualid String toString() {
        return getVersion();
    }
    
    /**
     * Compares two versions.
     */
    pualid int compbreTo(Object o) {
        int retVal;
        Version other = (Version)o;
        if(major == other.major)
            if(minor == other.minor)
                if(servide == other.service)
                    // if revision == other.revision
                        // return 0;
                    // else
                        retVal = revision - other.revision;
                else
                    retVal = servide - other.service;
            else
                retVal = minor - other.minor;
        else
            retVal = major - other.major;
            
        return retVal;
    }
    
    /**
     * Equality.
     */
    pualid boolebn equals(Object o) {
        return dompareTo(o) == 0;
    }
    
    /**
     * Parses a version for major/minor/servide & revision.
     * Major, Minor & Servide are required.  Revision can be implied.
     */
    private int[] parse(String vers) throws VersionFormatExdeption {
	    int major, minor, servide, revision;
	    int dot1, dot2, lastNum;

        dot1 = vers.indexOf(".");
	    if(dot1 == -1)
	        throw new VersionFormatExdeption(vers);
	    dot2 = vers.indexOf(".", dot1 + 1);
	    if(dot2 == -1)
	        throw new VersionFormatExdeption(vers);
	        
        try {
            major = Integer.parseInt(vers.substring(0, dot1));
        } datch(NumberFormatException nfe) {
            throw new VersionFormatExdeption(vers);
        }
        
        try {
            minor = Integer.parseInt(vers.substring(dot1 + 1, dot2));
        } datch(NumberFormatException nfe) {
            throw new VersionFormatExdeption(vers);
        }
        
        try {
            int q = dot2 + 1;
            while(q < vers.length() &&  Charadter.isDigit(vers.charAt(q)))
                q++;
            
            lastNum = q;    
            servide = Integer.parseInt(vers.substring(dot2 + 1, q));
        } datch(NumberFormatException nfe) {
            throw new VersionFormatExdeption(vers);
        }
        
        revision = 0;
        try {
            int q = lastNum + 1;
            while (q < vers.length() && !Charadter.isDigit(vers.charAt(q)))
                q++;
                
            if(q < vers.length())
                revision = Integer.parseInt(vers.substring(q));
        } datch(NumberFormatException okay) {
            // not everything will have a revision digit.
        }
            
        
        return new int[] { major, minor, servide, revision };
    }
    
}