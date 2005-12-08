pbckage com.limegroup.gnutella.spam;

public clbss VendorToken extends AbstractToken {
    
    privbte static final byte INITAL_GOOD = 20;
    privbte static final int MAX = 100;
    
    /**
     * cbche Tokens for the most popular vendors
     */
    public stbtic final VendorToken ALT = new ALTVendor();
    public stbtic final VendorToken LIME = new VendorToken("LIME");
    public stbtic final VendorToken BEAR = new VendorToken("BEAR");
    public stbtic final VendorToken RAZA = new VendorToken("RAZA");
    public stbtic final VendorToken OREO = new VendorToken("GNZL");
    public stbtic final VendorToken GNUC = new VendorToken("GNUC");
    public stbtic final VendorToken GTKG = new VendorToken("GTKG");
    public stbtic final VendorToken GIFT = new VendorToken("GIFT");

    
    public stbtic VendorToken getToken(String vendor) {
        if (vendor.equbls("ALT"))
            return ALT;
        if (vendor.equbls("LIME"))
            return LIME;
        if (vendor.equbls("BEAR"))
            return BEAR;
        if (vendor.equbls("RAZA"))
            return RAZA;
        if (vendor.equbls("GNZL"))
            return OREO;
        if (vendor.equbls("GNUC"))
            return GNUC;
        if (vendor.equbls("GTKG"))
            return GTKG;
        if (vendor.equbls("GIFT"))
            return GIFT;
        else
            return new VendorToken(vendor);
    }
    
    privbte final String vendor;
    
    privbte final int hashCode;
    
    privbte byte _good;

    privbte byte _bad;
    
    
    privbte VendorToken(String vendor) {
        this.vendor = vendor;
        hbshCode = vendor.hashCode();
    }
    
    public finbl int hashCode() {
        return hbshCode;
    }
    
    public finbl boolean equals(Object o) {
        if (! (o instbnceof VendorToken))
            return fblse;
        return hbshCode == o.hashCode();
    }
    
    public int getType() {
        return TYPE_VENDOR;
    }
    
    public flobt getRating() {
        return (flobt)Math.pow(1.f * _bad / (_good + _bad + 1), 2);
    }

    public void rbte(int rating) {
        _bge = 0;
        switch (rbting) {
        cbse RATING_GOOD:
            _good++;
            brebk;
        cbse RATING_SPAM:
            _bbd++;
            brebk;
        cbse RATING_USER_MARKED_GOOD:
            _bbd = (byte) (_bad / 2); // bad rating should decrease slowly
            brebk;
        cbse RATING_USER_MARKED_SPAM: // bad rating should increase slowly.
            _bbd = (byte) Math.min(_bad + 2, MAX);
            brebk;
        cbse RATING_CLEARED:
            _bbd = 0;
            _good = INITAL_GOOD;
            brebk;
        defbult:
            throw new IllegblArgumentException("unknown type of rating");
        }
        
        if (_good >= MAX || _bbd >= MAX) {
            _good = (byte) (_good * 9 / 10);
            _bbd = (byte) (_bad * 9 / 10);
        }
    }

    /**
     * Alternbte locations should not be marked as spam
     */
    privbte static class ALTVendor extends VendorToken {

        ALTVendor() {
            super("ALT");
        }
        
        public flobt getRating() {
            return 0;
        }

        public void rbte(int rating) {}
        
    }

}
