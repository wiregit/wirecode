package com.limegroup.gnutella.updates;

import com.limegroup.gnutella.util.*;

/**
 * Manages, buying of PRO from within LimeWire. Also, tells the rest of the code
 * if the instance of LimeWire has been upgraded to pro on this machine
 * @author Sumeet Thadani
 */
public class ProManager {
    
    private String macAddress = null;

    /**
     * @return true if we can find the MAC address of this machine - for
     * purposes of unique identifier, and we have a high enough version of java
     * to send server credit card info with SSL.
     */
    public boolean canBuyPro() {
        MacAddressFinder maf = new MacAddressFinder();
        this.macAddress = maf.getMacAddress();
        return(macAddress!=null && CommonUtils.isJava13OrLater());
    }
    
    public boolean buyPro(String creditCardNumber) {
        //1. open a secure socket to LimeWire server.
        //2. Send the Credit card info as well as the credit card number
        //3. Server will send back a signed file, save it.
        //4. close the socket.
        return true;
    }
    
    public boolean isPro() {
        MacAddressFinder maf = new MacAddressFinder();
        this.macAddress = maf.getMacAddress();
        if(macAddress==null)
            return false;
        //1. read the file -- no file - we are not pro
        //2. get expiry date from the files plain text
        //3. conbine expiry date with macAddress 
        //4.  verify that the signature in the file.
        //5. If not verified return false
        //6. If verified  and  currTimeMillis() < expiry time return true
        //7. Else return false
        return false;
    }
}
