package com.limegroup.gnutella.util;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella .*;
import java.io.*;
import java.net.*;

/**
 * This class handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class NetworkUtils {

    /**
     * Ensure that this class cannot be constructed.
     */
    private NetworkUtils() {}

	/**
	 * Returns whether or not the specified port is within the valid range of
	 * ports.
	 *
	 * @param port the port number to check
	 */
	public static boolean isValidPort(int port) {
		if((port & 0xFFFF0000) != 0) return false;
		return true;
	}
	
	/**
	 * Returns whether or not the supplied address is a local address.
	 */
	public static boolean isLocalAddress(InetAddress addr) {
	    try {
            InetAddress address = InetAddress.getLocalHost();
            byte[] byteAddress = addr.getAddress();
            return (address.equals(addr) ||
                    byteAddress[0] == 127);
        } catch(UnknownHostException e) {
            return false;
        }
    }

    /**
     * Checks to see if the given address is a firewalled address.
     * 
     * @param address the address to check
     */
    public static boolean isPrivateAddress(byte[] address) {
        if (address[0]==(byte)10) {
            return true;  //10.0.0.0 - 10.255.255.255
        } else if (address[0]==(byte)127 && 
                   ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            return true;  //127.x.x.x
        } else if (address[0]==(byte)172 &&
                   address[1]>=(byte)16 &&
                   address[1]<=(byte)31) {
            return true;  //172.16.0.0 - 172.31.255.255
        } else if (address[0]==(byte)192 &&
                   address[1]==(byte)168) {
            return true; //192.168.0.0 - 192.168.255.255
        } else if (address[0]==(byte)169 &&
                   address[1]==(byte)254) {
            return true; //169.254.0.0 - 169.254.255.255 (local link)
        } else if (address[0]==(byte)0) {
            return true; //0.0.0.0 -- reserved 
            //} else if (address[0]>=(byte)240) {
            //return true; //240 and above -- broadcast, multicast
        } else {
            return false; // otherwise, it's not private
        }
    }

    /** 
     * Returns the ip (given in BIG-endian) format as standard
     * dotted-decimal, e.g., 192.168.0.1<p> 
     *
     * @param ip the ip address in BIG-endian format
     * @return the IP address as a dotted-quad string
     */
     public static final String ip2string(byte[] ip) {
         return ip2string(ip, 0);
     }
         
    /** 
     * Returns the ip (given in BIG-endian) format of
     * buf[offset]...buf[offset+3] as standard dotted-decimal, e.g.,
     * 192.168.0.1<p> 
     *
     * @param ip the IP address to convert
     * @param offset the offset into the IP array to convert
     * @return the IP address as a dotted-quad string
     */
    public static final String ip2string(byte[] ip, int offset) {
        // xxx.xxx.xxx.xxx => 15 chars
        StringBuffer sbuf = new StringBuffer(16);   
        sbuf.append(ByteOrder.ubyte2int(ip[offset]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(ip[offset+1]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(ip[offset+2]));
        sbuf.append('.');
        sbuf.append(ByteOrder.ubyte2int(ip[offset+3]));
        return sbuf.toString();
    }
}



