package com.limegroup.gnutella.messages.vendor;

/**
 * Pseudo-vendor message, used to reserve space in the 
 * "Messages Supported VM" hierarchy 
 * 
 * Clients showing support for this message will, depending upon whether they
 * are UP or leaf:
 * <p><tt>
 *<br> 		WE----THEY:	Effect on LastHop QRTs
 *<br> 		----------:	----------------------
 *<br> 		UP------UP: Does nothing
 *<br>		UP----Leaf:	When we receive a BEAR4v1 HopsFlow of 0, we will remove client's QRP
 *<br> 		Leaf----UP:	When we send a BEAR4v1 HopsFlow of 0, we would like our QRT removed
 * </tt>	 
 */
public class BusyLeafVendorMessage extends Object {

    public static final int VERSION = 1; 
    
    /**
     * private to prevent creation, this class stores nothing
     */
    private BusyLeafVendorMessage(){}
    
}
