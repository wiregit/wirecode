package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.filters.IP;
import com.limegroup.gnutella.filters.IPList;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class NetworkUtils {
    
    /**
     * The list of invalid addresses.
     */
    private static final IPList INVALID_ADDRESSES = new IPList();
    
    /**
     * The list of private addresses.
     */
    private static final IPList PRIVATE_ADDRESSES = new IPList();
    
    /**
     * The list of local addresses.
     */
    private static final IPList LOCAL_ADDRESSES = new IPList();
    
    static {
        INVALID_ADDRESSES.add("0.*/8");
        INVALID_ADDRESSES.add("255.*/8");
        
        PRIVATE_ADDRESSES.add("0.*/8");
        PRIVATE_ADDRESSES.add("127.*/8");
        PRIVATE_ADDRESSES.add("255.*/8");
        PRIVATE_ADDRESSES.add("10.*/8");
        PRIVATE_ADDRESSES.add("172.16.*/12");
        PRIVATE_ADDRESSES.add("169.254.*/16");
        PRIVATE_ADDRESSES.add("192.168.*/16");
        
        LOCAL_ADDRESSES.add("127.*/8");
    }


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
        if(port == 0) return false;
		return true;
	}
	
	/**
	 * Returns whether or not the specified address is valid.
	 */
	public static boolean isValidAddress(byte[] addr) {
	    return !INVALID_ADDRESSES.contains(new IP(addr));
    }
    
    /**
     * Returns whether or not the specified InetAddress is valid.
     */
    public static boolean isValidAddress(InetAddress addr) {
        return isValidAddress(addr.getAddress());
    }
    
    /**
     * Returns whether or not the specified host is a valid address.
     */
    public static boolean isValidAddress(String host) {
        try {
            return isValidAddress(InetAddress.getByName(host));
        } catch(UnknownHostException uhe) {
            return false;
        }
    }
	
	/**
	 * Returns whether or not the supplied address is a local address.
	 */
	public static boolean isLocalAddress(InetAddress addr) {
	    try {
	        if( LOCAL_ADDRESSES.contains(new IP(addr.getAddress())) )
	            return true;

            InetAddress address = InetAddress.getLocalHost();
            return Arrays.equals(address.getAddress(), addr.getAddress());
        } catch(UnknownHostException e) {
            return false;
        }
    }

    /**
     * Returns whether or not the two ip addresses share the same
     * first octet in their address.  
     *
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isCloseIP(byte[] addr0, byte[] addr1) {
        return addr0[0] == addr1[0];        
    }

    /**
     * Returns whether or not the two ip addresses share the same
     * first two octets in their address -- the most common
     * indication that they may be on the same network.
     *
     * Private networks are NOT CONSIDERED CLOSE.
     *
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        // if 0 is not a private address but 1 is, then the next
        // check will fail anyway, so this is okay.
        if( isPrivateAddress(addr0) )
            return false;
        else 
            return
                addr0[0] == addr1[0] &&
                addr0[1] == addr1[1];
    }

    /**
     * Returns whether or not the given ip address shares the same
     * first three octets as the address for this node -- the most 
     * common indication that they may be on the same network.
     *
     * @param addr the address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr) {
        return isVeryCloseIP(RouterService.getAddress(), addr);
    }
    
    /**
     * Returns whether or not this node has a private address.
     *
     * @return <tt>true</tt> if this node has a private address,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivate() {
        return isPrivateAddress(RouterService.getAddress());
    }

    /**
     * Checks to see if the given address is a firewalled address.
     * 
     * @param address the address to check
     */
    public static boolean isPrivateAddress(byte[] address) {
        if( !ConnectionSettings.LOCAL_IS_PRIVATE.getValue() )
            return false;
            
        return PRIVATE_ADDRESSES.contains(new IP(address));
    }

    /**
     * Utility method for determing whether or not the given 
     * address is private taking an InetAddress object as argument
     * like the isLocalAddress(InetAddress) method. Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivateAddress(InetAddress address) {
        return isPrivateAddress(address.getAddress());
    }

    /**
     * Utility method for determing whether or not the given 
     * address is private.  Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * Returns true if the host is unknown.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivateAddress(String address) {
        try {
            return isPrivateAddress(InetAddress.getByName(address));
        } catch(UnknownHostException uhe) {
            return true;
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
    



    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>false</tt>.
     */
    public static boolean isMe(String host, int port) {
        byte[] cIP;
        try {
            cIP = InetAddress.getByName(host).getAddress();
        } catch (IOException e) {
            return false;
        }
        
        return isMe(cIP, port);
    }
    
    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>false</tt>.
     */
    public static boolean isMe(byte[] cIP, int port) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "127.*.*.*" since
        //they are aliases this machine.

        if (cIP[0]==(byte)127) {
            return port == RouterService.getPort();
        } else {
            byte[] managerIP = RouterService.getAddress();
            return port == RouterService.getPort() &&
                   Arrays.equals(cIP, managerIP);
        }
    }
    
    /**
     * Packs a Collection of IpPorts into a byte array.
     */
    public static byte[] packIpPorts(Collection ipPorts) {
        byte[] data = new byte[ipPorts.size() * 6];
        int offset = 0;
        for(Iterator i = ipPorts.iterator(); i.hasNext(); ) {
            IpPort next = (IpPort)i.next();
            byte[] addr = next.getInetAddress().getAddress();
            int port = next.getPort();
            System.arraycopy(addr, 0, data, offset, 4);
            offset += 4;
            ByteOrder.short2leb((short)port, data, offset);
            offset += 2;
        }
        return data;
    }
    
    /**
     * parses an ip:port byte-packed values.  
     * 
     * @return a collection of <tt>IpPort</tt> objects.
     * @throws BadPacketException if an invalid Ip is found or the size 
     * is not divisble by six
     */
    public static List unpackIps(byte [] data) throws BadPacketException {
    	if (data.length % 6 != 0)
    		throw new BadPacketException("invalid size");
    	
    	int size = data.length/6;
    	List ret = new ArrayList(size);
    	byte [] current = new byte[6];
    	
    	
    	for (int i=0;i<size;i++) {
    		System.arraycopy(data,i*6,current,0,6);
    		ret.add(QueryReply.IPPortCombo.getCombo(current));
    	}
    	
    	return Collections.unmodifiableList(ret);
    }

    /**
     * 
     * @return a collection of <tt>PushEndpoint</tt> objects
     * @throws BadPacketException if parsing failed.
     */
    public static List unpackPushEPs(byte [] data) 
    	throws BadPacketException {
    	List ret = new LinkedList();
    	
    	int i=0;
    	while (i < data.length ) {
    		PushEndpoint current = PushEndpoint.fromBytes(data,i);
    		i+=PushEndpoint.getSizeBytes(current.getProxies());
    		if (!current.getAddress().equals(RemoteFileDesc.BOGUS_IP))
    		    i=6;
    		ret.add(current);
    	}
    	
    	return Collections.unmodifiableList(ret);
    }
    
    /**
     * Returns an InetAddress representing the given IP address.
     */
    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        String addrString = NetworkUtils.ip2string(addr);
        return InetAddress.getByName(addrString);
    }
}



