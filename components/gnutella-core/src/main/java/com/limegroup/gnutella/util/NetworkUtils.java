package com.limegroup.gnutella.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.IPPortCombo;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class NetworkUtils {
    
    /**
     * The list of invalid addresses.
     */
    private static final byte [] INVALID_ADDRESSES_BYTE = 
        new byte[]{(byte)0,(byte)255};
    
    /**
     * The list of private addresses.
     */
    private static final int [][] PRIVATE_ADDRESSES_BYTE =
        new int[][]{
            {0xFF000000,0},
            {0xFF000000,127 << 24},
            {0xFF000000,255 << 24},
            {0xFF000000,10 << 24},
            {0xFFF00000,(172 << 24) | (16 << 16)},
            {0xFFFF0000,(169 << 24) | (254 << 16)},
            {0xFFFF0000,(192 << 24) | (168 << 16)}};
    
    
    /**
     * The list of local addresses.
     */
    private static final byte LOCAL_ADDRESS_BYTE = (byte)127;
    
    /**
     * Ensure that this class cannot be constructed.
     */
    private NetworkUtils() {}
    
    /**
     * Determines if the given addr or port is valid.
     * Both must be valid for this to return true.
     */
    public static boolean isValidAddressAndPort(byte[] addr, int port) {
        return isValidAddress(addr) && isValidPort(port);
    }
    
    /**
     * Determines if the given addr or port is valid.
     * Both must be valid for this to return true.
     */
    public static boolean isValidAddressAndPort(String addr, int port) {
        return isValidAddress(addr) && isValidPort(port);
    }    

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
	    return addr[0]!=INVALID_ADDRESSES_BYTE[0] &&
	    	addr[0]!=INVALID_ADDRESSES_BYTE[1];
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
	        if( addr.getAddress()[0]==LOCAL_ADDRESS_BYTE )
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
        
        
        int addr = ((address[0] & 0xFF) << 24) | 
        			((address[1] & 0xFF)<< 16);
        
        for (int i =0;i< 7;i++){
            if ((addr & PRIVATE_ADDRESSES_BYTE[i][0]) ==
                	PRIVATE_ADDRESSES_BYTE[i][1])
                return true;
        }
        
        return false;
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
    
    public static boolean isMe(IpPort me) {
    	if (me == IpPortForSelf.instance())
    		return true;
    	return isMe(me.getInetAddress().getAddress(),me.getPort());
    }

    /**
     * Determines if the given socket is from a local host.
     */
    public static boolean isLocalHost(Socket s) {
        String hostAddress = s.getInetAddress().getHostAddress();
        return "127.0.0.1".equals(hostAddress);
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
    		ret.add(IPPortCombo.getCombo(current));
    	}
    	
    	return Collections.unmodifiableList(ret);
    }

    public static List unpackPushEPs(InputStream is) throws BadPacketException,IOException {
        List ret = new LinkedList();
        DataInputStream dais = new DataInputStream(is);
        while (dais.available() > 0) 
            ret.add(PushEndpoint.fromBytes(dais));
        
        return Collections.unmodifiableList(ret);
    }
    
    /**
     * Returns an InetAddress representing the given IP address.
     */
    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        String addrString = NetworkUtils.ip2string(addr);
        return InetAddress.getByName(addrString);
    }
    
    /**
     * @return whether the IpPort is a valid external address.
     */
    public static boolean isValidExternalIpPort(IpPort addr) {
        if (addr == null)
            return false;
	byte [] b = addr.getInetAddress().getAddress();       
        return isValidAddress(b) &&
        	!isPrivateAddress(b) &&
        	isValidPort(addr.getPort());
    }
    
    /**
     * @return A non-loopback IPv4 address of a network interface on the local
     *         host.
     * @throws UnknownHostException
     */
    public static InetAddress getLocalAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        
        if (addr instanceof Inet4Address 
                && !addr.isLoopbackAddress()) {
            return addr;
        }
        
        try {
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();

            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    Enumeration addresses = ((NetworkInterface)interfaces.nextElement()).getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        addr = (InetAddress)addresses.nextElement();
                        if (addr instanceof Inet4Address
                                && !addr.isLoopbackAddress()) {
                            return addr;
                        }
                    }
                }
            }
        } catch (SocketException se) {
        }

        throw new UnknownHostException(
                "localhost has no interface with a non-loopback IPv4 address");
    }
}



