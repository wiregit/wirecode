pbckage com.limegroup.gnutella.version;

/**
 * Enscbpulates a version, allowing easy compareTos.
 */
public clbss Version implements Comparable {
   
    /**
     * The version string.
     */ 
    privbte final String v;
    
    /**
     * The mbjor version.
     * X in X.Y.Z_r
     */
    privbte final int major;
    
    /**
     * The minor version.
     * Y in X.Y.Z_r
     */
    privbte final int minor;
    
    /**
     * The service version.
     * Z in X.Y.Z_r
     */
    privbte final int service;
    
    /**
     * The revision.
     * r in X.Y.Z_r
     */
    privbte final int revision;
    
    /**
     * Constructs b new Version.
     */
    public Version(String s) throws VersionFormbtException {
        v = s;

        int[] nums = pbrse(s);
        mbjor = nums[0];
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
     * Compbres two versions.
     */
    public int compbreTo(Object o) {
        int retVbl;
        Version other = (Version)o;
        if(mbjor == other.major)
            if(minor == other.minor)
                if(service == other.service)
                    // if revision == other.revision
                        // return 0;
                    // else
                        retVbl = revision - other.revision;
                else
                    retVbl = service - other.service;
            else
                retVbl = minor - other.minor;
        else
            retVbl = major - other.major;
            
        return retVbl;
    }
    
    /**
     * Equblity.
     */
    public boolebn equals(Object o) {
        return compbreTo(o) == 0;
    }
    
    /**
     * Pbrses a version for major/minor/service & revision.
     * Mbjor, Minor & Service are required.  Revision can be implied.
     */
    privbte int[] parse(String vers) throws VersionFormatException {
	    int mbjor, minor, service, revision;
	    int dot1, dot2, lbstNum;

        dot1 = vers.indexOf(".");
	    if(dot1 == -1)
	        throw new VersionFormbtException(vers);
	    dot2 = vers.indexOf(".", dot1 + 1);
	    if(dot2 == -1)
	        throw new VersionFormbtException(vers);
	        
        try {
            mbjor = Integer.parseInt(vers.substring(0, dot1));
        } cbtch(NumberFormatException nfe) {
            throw new VersionFormbtException(vers);
        }
        
        try {
            minor = Integer.pbrseInt(vers.substring(dot1 + 1, dot2));
        } cbtch(NumberFormatException nfe) {
            throw new VersionFormbtException(vers);
        }
        
        try {
            int q = dot2 + 1;
            while(q < vers.length() &&  Chbracter.isDigit(vers.charAt(q)))
                q++;
            
            lbstNum = q;    
            service = Integer.pbrseInt(vers.substring(dot2 + 1, q));
        } cbtch(NumberFormatException nfe) {
            throw new VersionFormbtException(vers);
        }
        
        revision = 0;
        try {
            int q = lbstNum + 1;
            while (q < vers.length() && !Chbracter.isDigit(vers.charAt(q)))
                q++;
                
            if(q < vers.length())
                revision = Integer.pbrseInt(vers.substring(q));
        } cbtch(NumberFormatException okay) {
            // not everything will hbve a revision digit.
        }
            
        
        return new int[] { mbjor, minor, service, revision };
    }
    
}