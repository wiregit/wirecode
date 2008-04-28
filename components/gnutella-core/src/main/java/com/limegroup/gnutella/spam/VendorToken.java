package com.limegroup.gnutella.spam;



public class VendorToken extends AbstractToken {
    
    private static final long serialVersionUID = -3593261726550970847L;
    
    private static final byte INITAL_GOOD = 20;
    private static final int MAX = 100;
    
    /**
     * cache Tokens for the most popular vendors
     */
    public static final VendorToken ALT = new ALTVendor();
    public static final VendorToken LIME = new VendorToken("LIME");
    public static final VendorToken BEAR = new VendorToken("BEAR");
    public static final VendorToken RAZA = new VendorToken("RAZA");
    public static final VendorToken OREO = new VendorToken("GNZL");
    public static final VendorToken GNUC = new VendorToken("GNUC");
    public static final VendorToken GTKG = new VendorToken("GTKG");
    public static final VendorToken GIFT = new VendorToken("GIFT");

    
    public static VendorToken getToken(String vendor) {
        if (vendor.equals("ALT"))
            return ALT;
        if (vendor.equals("LIME"))
            return LIME;
        if (vendor.equals("BEAR"))
            return BEAR;
        if (vendor.equals("RAZA"))
            return RAZA;
        if (vendor.equals("GNZL"))
            return OREO;
        if (vendor.equals("GNUC"))
            return GNUC;
        if (vendor.equals("GTKG"))
            return GTKG;
        if (vendor.equals("GIFT"))
            return GIFT;
        else
            return new VendorToken(vendor);
    }
    
    private final String vendor;
    
    private final int hashCode;
    
    private byte _good;

    private byte _bad;
    
    
    private VendorToken(String vendor) {
        this.vendor = vendor;
        hashCode = vendor.hashCode();
    }
    
    @Override
    public final int hashCode() {
        return hashCode;
    }
    
    @Override
    public final boolean equals(Object o) {
        if (! (o instanceof VendorToken))
            return false;
        
        if (hashCode != o.hashCode()) {
            return false;
        }
        
        return vendor.equals(((VendorToken)o).vendor);
    }
    
    public TokenType getType() {
        return TokenType.VENDOR;
    }
    
    public float getRating() {
        return (float)Math.pow(1.f * _bad / (_good + _bad + 1), 2);
    }

    public void rate(Rating rating) {
        _age = 0;
        switch (rating) {
        case PROGRAM_MARKED_GOOD:
            _good++;
            break;
        case PROGRAM_MARKED_SPAM:
            _bad++;
            break;
        case USER_MARKED_GOOD:
            _bad = (byte) (_bad / 2); // bad rating should decrease slowly
            break;
        case USER_MARKED_SPAM: // bad rating should increase slowly.
            _bad = (byte) Math.min(_bad + 2, MAX);
            break;
        case CLEARED:
            _bad = 0;
            _good = INITAL_GOOD;
            break;
        default:
            throw new IllegalArgumentException("unknown type of rating");
        }
        
        if (_good >= MAX || _bad >= MAX) {
            _good = (byte) (_good * 9 / 10);
            _bad = (byte) (_bad * 9 / 10);
        }
    }

    /**
     * Alternate locations should not be marked as spam
     */
    private static class ALTVendor extends VendorToken {

        private static final long serialVersionUID = 703031386150175127L;

        ALTVendor() {
            super("ALT");
        }
        
        @Override
        public float getRating() {
            return 0;
        }

        @Override
        public void rate(Rating rating) {}
        
    }

}
