
// Edited for the Learning branch

package com.limegroup.gnutella.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
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
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class handles common utility functions for networking tasks.
 */
public final class NetworkUtils {

    /** An array that contains 0 and 255, use it to notice an IP address that starts 0 or 255, which is invalid. */
    private static final byte[] INVALID_ADDRESSES_BYTE = new byte[] { (byte)0, (byte)255 };

    /**
     * A list of 7 mask and address pairs you can use to determine if an IP address is just an internal LAN address.
     * 
     * This is a two dimensional array, int[][].
     * It's a grid with 7 rows and 2 columns.
     * 
     * The first column contains masks, and the second column contains IP addresses.
     * They go together, for instance, PRIVATE_ADDRESS_BYTE[1][1] masks out the first byte, and PRIVATE_ADDRESS_BYTE[1][2] is the address 127.0.0.0.
     * 
     * This is used in just one place, the isPrivateAddress() method.
     * If an IP address doesn't match any of the 7 tests here, it's a real external Internet IP address.
     */
    private static final int[][] PRIVATE_ADDRESSES_BYTE = new int[][] {

    	// Put 7 pairs of masks and addresses in the array
    	{0xFF000000,    0},                       // Index 0: First byte,   0.0.0.0
        {0xFF000000,  127 << 24},                 // Index 1: First byte, 127.0.0.0
        {0xFF000000,  255 << 24},                 // Index 2: First byte, 255.0.0.0
        {0xFF000000,   10 << 24},                 // Index 3: First byte,  10.0.0.0
        {0xFFF00000, (172 << 24) |  (16 << 16)},  // Index 4: First byte and a half, 0xAC100000
        {0xFFFF0000, (169 << 24) | (254 << 16)},  // Index 5: First two bytes, 169.254.0.0
        {0xFFFF0000, (192 << 24) | (168 << 16)}}; // Index 6: First two bytes, 192.168.0.0

    /** If an IP address starts 127, it's a LAN address. */
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
	 * Returns true if port is 1 through 65535
	 * Returns false if port is 0, 65536, or higher
	 *
	 * @param port the port number to check
	 */
	public static boolean isValidPort(int port) {
		if((port & 0xFFFF0000) != 0) return false;
        if(port == 0) return false;
		return true;
	}

	/**
	 * Returns false if the IP address starts 0 or 255.
	 * This is the isValidAddress method which the others call, and which actually looks at the IP address.
	 * 
	 * @param addr An array of 4 bytes that is an IP address
	 * @return     True if it looks OK, false if it starts with a 0 or 255
	 */
	public static boolean isValidAddress(byte[] addr) {

		// Only return true if the first number in the IP address isn't 0 or 255
	    return addr[0] != INVALID_ADDRESSES_BYTE[0] && addr[0] != INVALID_ADDRESSES_BYTE[1];
    }

    /**
	 * Returns false if the IP address starts 0 or 255.
	 * 
	 * @param addr An InetAddress object
	 * @return     True if it looks OK, false if it starts with a 0 or 255
     */
    public static boolean isValidAddress(InetAddress addr) {

    	// Get the 4 bytes inside the InetAddress object, and pass them to the other isValidAddress method
        return isValidAddress(addr.getAddress());
    }

    /**
	 * Returns false if the IP address starts 0 or 255.
	 * 
	 * @param addr A string like "216.27.178.74"
	 * @return     True if it looks OK, false if it starts with a 0 or 255
     */
    public static boolean isValidAddress(String host) {

        try {

        	// Convert the given text into an InetAddress object, and pass it to the other isValidAddress method
            return isValidAddress(InetAddress.getByName(host));

        // getByName threw an exception, say the IP address isn't valid
        } catch (UnknownHostException uhe) { return false; }
    }

	/**
	 * True if the given address starts 127 or is our own address on the LAN.
	 * 
	 * @param addr Address to look at and determine if it's local
	 * @return     True if it's a LAN IP address, false if it's an Internet IP address
	 */
	public static boolean isLocalAddress(InetAddress addr) {

		try {

			// If the given address starts 127, it's local
	        if (addr.getAddress()[0] == LOCAL_ADDRESS_BYTE) return true;

            // If the given address is our address, it's local
	        InetAddress address = InetAddress.getLocalHost(); // Ask Java for our IP address on the LAN, like 192.168.0.102
            return Arrays.equals(address.getAddress(), addr.getAddress());

        } catch (UnknownHostException e) {

        	// Some error happened, say the address is external
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
     * True if the IP address we're telling remote computers is just a LAN address.
     *  
     * @return True if our IP address is a LAN address, false if it's an Internet IP address.
     */
    public static boolean isPrivate() {

    	// Get the address we're telling remote computers is ours, and see if it matches any of the 7 ranges of LAN IP addresses
        return isPrivateAddress(RouterService.getAddress());
    }

    /**
     * Sees if a given IP address is in one of the ranges of addresses that DHCP servers assign to computers on their LAN.
     * For instance, 192.168.1.102 is a LAN address, while 216.27.158.74 is not.
     * This is the method that can identify a LAN IP address just by looking at it.
     * 
     * If a remote computer tells us its IP address and it's just a LAN address, this means that computer is behind a NAT.
     * 
     * @param address An IP address as an array of 4 bytes
     * @return        True if it's a LAN IP address, false if it's an Internet IP address
     */
    public static boolean isPrivateAddress(byte[] address) {

    	// If settings allow us to make Gnutella connections to other computers on our LAN, then there's no such thing as a private address
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) return false; // Connect to this IP as though it were public and distant on the Internet

        /*
         * Both the byte[] address and an int are 4 bytes big.
         * Copy just the first two bytes from the IP address into an int, and then read the int as a number.
         * For instance, the IP address "1.2.3.4" is a byte array like 0x01 0x02 0x03 0x04.
         * The next line of code clips out the first half, like        0x01 0x02 0x00 0x00.
         * Then, it reads it as a number, like 0x01020000, which is the number 16908288.
         */

        // Clip out the first half of the IP address like x.x.0.0
        int addr = ((address[0] & 0xFF) << 24)  // Take the first byte of the IP address, and shift it to the left 3 bytes
                 | ((address[1] & 0xFF) << 16); // Take the second byte of the IP address, and shift it to the left 2 bytes

        // Loops 7 times, once for each of the 7 mask and IP address pairs in the list
        for (int i = 0; i < 7; i++) {

        	// If the parts of the given address shown with the mask equal the correpsonding address, it's a LAN address
            if ((addr & PRIVATE_ADDRESSES_BYTE[i][0]) == PRIVATE_ADDRESSES_BYTE[i][1]) return true;
        }

        // It passed all 7 tests, it's a Internet IP address
        return false;
    }

    /**
     * Sees if a given IP address is in one of the ranges of addresses that DHCP servers assign to computers on their LAN.
     * For instance, 192.168.1.102 is a LAN address, while 216.27.158.74 is not.
     * This is the method that can identify a LAN IP address just by looking at it.
     * 
     * If a remote computer tells us its IP address and it's just a LAN address, this means that computer is behind a NAT.
     * 
     * @param address An IP address as an InetAddress object
     * @return        True if it's a LAN IP address, false if it's an Internet IP address
     */
    public static boolean isPrivateAddress(InetAddress address) {

    	// Get the array of 4 bytes from the InetAddress object, and pass them to the other method
        return isPrivateAddress(address.getAddress());
    }

    /**
     * Sees if a given IP address is in one of the ranges of addresses that DHCP servers assign to computers on their LAN.
     * For instance, 192.168.1.102 is a LAN address, while 216.27.158.74 is not.
     * This is the method that can identify a LAN IP address just by looking at it.
     * 
     * If a remote computer tells us its IP address and it's just a LAN address, this means that computer is behind a NAT.
     * 
     * @param address An IP address as a string like "192.168.1.102"
     * @return        True if it's a LAN IP address or there was an error reading it, false if it's an Internet IP address
     */
    public static boolean isPrivateAddress(String address) {

    	try {
    		
    		// Convert the string into an InetAddress object, and call the other method
    		return isPrivateAddress(InetAddress.getByName(address));

    	// Reading the text caused an exception, report that this is a LAN address
    	} catch (UnknownHostException uhe) { return true; }
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
    		ret.add(QueryReply.IPPortCombo.getCombo(current));
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
}



