package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized.
 *
 * @author Gregorio Roper 
 */
class IPList {
    /** The list of IP's. */
    private Vector /* of IP */ ips = new Vector();
    private static final long DEFAULT_MASK = 0xffffffffL; // 255.255.255.255 

    /** 
     * As an optimization, we used to store the logical AND of all addresses and
     * inverted addresses in ips:
     * 
     *  INVARIANT: pmask=DEFAULT_MASK &  ips[0].addr  &  ips[1].addr ...
     *             nmask=DEFAULT_MASK & ~ips[0].addr) & ~ips[1].addr ...
     *
     * With this information, we can quickly tell that an address x is NOT in
     * this if x&pmask!=pmask or (~x)&nmask~=nmask, i.e., if x has a 1 or a 0
     * where no element of ips had one.  Confirming that x is actually in this
     * requires a linear search.  In this way, pmask/nmask act like a sort of
     * Bloom Filter.
     *
     * However, this optimization is disabled at the moment because it has
     * little payoff in the common case, greatly complicates the code, and
     * makes assumptions about the implementation of IP.contains.  
     */
    //private long pmask = DEFAULT_MASK;   //positive mask
    //private long nmask = DEFAULT_MASK;   //negative mask

    public IPList () {}

    /** 
     * Removes a certain IP from the list.
     * @param string equal to the IP to remove */
    public void removeElement (String ip_str) {
        try {
            removeElement (new IP(ip_str));
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    /** 
     * Removes a certain IP from the list.
     * @param IP equal to the IP to remove
     */
    public void removeElement (IP ip) {
        ips.removeElement(ip);
        refreshMask();
    }
 
    /** 
     * Adds a certain IP from the list.
     * @param string equal to the IP to add
     */
    public void add(String ip_str) {
        try {
            add (new IP(ip_str));
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    /** 
     * Adds a certain IP from the list.
     * @param IP equal to the IP to add
     */
    public void add(IP ip) {
        if (ips.contains(ip))
            return;
        ips.addElement(ip);
        refreshMask (ip);
    }

    /**
     * @param IP to add to the mask
     */
    private void refreshMask (IP ip) {
        ////Currently disabled for simplicity.  See overview for details.
        //
        //pmask = ip.getMaskedAddr() & pmask;
        //nmask = ip.getIMaskedAddr() & nmask;
    }
    
    /**
     * Completely rebuild mask
     */
    private void refreshMask () {
        ////Currently disabled for simplicity.  See overview for details.
        //
        //this.pmask = DEFAULT_MASK;
        //this.nmask = DEFAULT_MASK;
        // 
        //for (int i = 0; i < ips.size(); i++) {
        //    refreshMask ((IP)ips.elementAt(i));
        //}
    }
    
    /**
     * Removes all IPs
     */
    public void clear () {
        removeAllElements();
    }
    public void removeAllElements () {
        ips.removeAllElements();
        refreshMask ();
    }

    /**
     * @param String equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains (String ip_str) {
        try {
            return  contains(new IP(ip_str));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @param String[] equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains (String[] ip_str) {
        try {
            return contains(new IP(ip_str));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @param IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains (IP ip) {
        ////The following optimization is currently disabled for simplicity.
        //
        //if ( ( (ip.getMaskedAddr() & pmask) != pmask) 
        //     || ( (ip.getIMaskedAddr() & nmask) != nmask)) // enhances speed
        //    return false;

        for (Iterator iter=ips.iterator(); iter.hasNext(); ) {
            IP pattern=(IP)iter.next();
            if (pattern.contains(ip))
                return true;
        }
        return false;
    }


    // Unit tests
    /*
    public static void main (String[] args) {
        IPList iplist = new IPList();

        iplist.add ("255.255.255.255");
        iplist.add ("0.0.0.0");
        iplist.add ("255.255.255.255");
        iplist.add ("255.255.0.255");
        iplist.add ("255.255.255.0");
        iplist.add ("0.255.255.255");
        iplist.add ("255.255.0.0");
        iplist.add ("0.255.0.255");
        iplist.add ("192.168.0.1/255.255.255.0");
        iplist.add ("10.0.*.*");

        Assert.that(iplist.contains ("0.0.0.0"));
        Assert.that(iplist.contains ("255.255.255.255"));
        Assert.that(iplist.contains ("255.255.255.0"));
        Assert.that(iplist.contains ("192.168.0.2"));
        Assert.that(iplist.contains ("192.168.0.1"));
        Assert.that(!iplist.contains ("192.168.1.1"));
        Assert.that(iplist.contains ("10.0.1.1"));
        Assert.that(!iplist.contains ("10.1.0.1"));

        iplist.removeElement ("0.0.0.0");

        Assert.that(!iplist.contains ("0.0.0.0"));
        Assert.that(iplist.contains ("255.255.255.255"));
        Assert.that(iplist.contains ("255.255.255.0"));
        Assert.that(iplist.contains ("192.168.0.2"));
        Assert.that(iplist.contains ("192.168.0.1"));
        Assert.that(!iplist.contains ("192.168.1.1"));
        Assert.that(iplist.contains ("10.0.1.1"));
        Assert.that(!iplist.contains ("10.1.0.1"));

        iplist.removeElement ("255.255.255.255");

        Assert.that(!iplist.contains ("0.0.0.0"));
        Assert.that(!iplist.contains ("255.255.255.255"));
        Assert.that(iplist.contains ("255.255.255.0"));
        Assert.that(iplist.contains ("192.168.0.2"));
        Assert.that(iplist.contains ("192.168.0.1"));
        Assert.that(!iplist.contains ("192.168.1.1"));
        Assert.that(iplist.contains ("10.0.1.1"));
        Assert.that(!iplist.contains ("10.1.0.1"));

        iplist.removeElement ("192.168.0.1");

        Assert.that(!iplist.contains ("0.0.0.0"));
        Assert.that(!iplist.contains ("255.255.255.255"));
        Assert.that(iplist.contains ("255.255.255.0"));
        Assert.that(iplist.contains ("192.168.0.2"));
        Assert.that(iplist.contains ("192.168.0.1"));
        Assert.that(!iplist.contains ("192.168.1.1"));
        Assert.that(iplist.contains ("10.0.1.1"));
        Assert.that(!iplist.contains ("10.1.0.1"));

        iplist.removeElement ("192.168.0.*");

        Assert.that(!iplist.contains ("0.0.0.0"));
        Assert.that(!iplist.contains ("255.255.255.255"));
        Assert.that(iplist.contains ("255.255.255.0"));
        Assert.that(!iplist.contains ("192.168.0.2"));
        Assert.that(!iplist.contains ("192.168.0.1"));
        Assert.that(!iplist.contains ("192.168.1.1"));
        Assert.that(iplist.contains ("10.0.1.1"));
        Assert.that(!iplist.contains ("10.1.0.1"));

        iplist.removeElement ("10.0.0.0/255.255.0.0");

        Assert.that(!iplist.contains ("0.0.0.0"));
        Assert.that(!iplist.contains ("255.255.255.255"));
        Assert.that(iplist.contains ("255.255.255.0"));
        Assert.that(!iplist.contains ("192.168.0.2"));
        Assert.that(!iplist.contains ("192.168.0.1"));
        Assert.that(!iplist.contains ("192.168.1.1"));
        Assert.that(!iplist.contains ("10.0.1.1"));
        Assert.that(!iplist.contains ("10.1.0.1"));
    }
    */
}
