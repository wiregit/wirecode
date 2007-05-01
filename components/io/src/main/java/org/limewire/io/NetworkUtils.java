package org.limewire.io;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.util.ByteOrder;

/**
 * This class handles common utility functions for networking tasks.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class NetworkUtils {
    
    /**
     * Natmask for Class C Networks
     */
    public static final int CLASS_C_NETMASK = 0xFFFFFF00;
    
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
     * @param port
     *            the port number to check
     */
    public static boolean isValidPort(int port) {
        return (port > 0 && port <= 0xFFFF);
    }
	
    /**
     * Returns whether or not the specified address is valid.
     * 
     * This method is IPv6 compliant
     */
    public static boolean isValidAddress(byte[] address) {
        return !isAnyLocalAddress(address) 
            && !isBroadcastAddress(address)
            && !isDocumentationAddress(address);
    }
    
    /**
     * Returns whether or not the specified IP is valid.
     */
    public static boolean isValidAddress(IP ip) {
        int msb = (ip.addr >> 24) & 0xFF;
        return (msb != 0x00 && msb != 0xFF);
    }
    
    /**
     * Returns whether or not the specified InetAddress is valid.
     */
    public static boolean isValidAddress(InetAddress address) {
        return !address.isAnyLocalAddress() 
            && !isBroadcastAddress(address)
            && !isDocumentationAddress(address);
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
     * @return whether the IpPort is a valid external address.
     */
    public static boolean isValidExternalIpPort(IpPort addr) {
        InetAddress address = addr.getInetAddress();
        return isValidAddress(address) 
            && isValidPort(addr.getPort())
            && !isPrivateAddress(address);
    }
    
    /**
     * Returns whether or not the specified InetAddress and Port is valid.
     */
    public static boolean isValidSocketAddress(SocketAddress address) {
        InetSocketAddress iaddr = (InetSocketAddress)address;
        
        return !iaddr.isUnresolved()
            && isValidAddress(iaddr.getAddress())
            && isValidPort(iaddr.getPort());
    }
    
    /**
     * Returns true if the InetAddress is any of our local machine addresses
     * 
     * This method is IPv6 compliant
     */
    public static boolean isLocalAddress(InetAddress addr) {
        // There are cases where InetAddress.getLocalHost() returns addresses
        // such as 127.0.1.1 (note the two 1) but if you iterate through all
        // NetworkInterfaces and look at every InetAddress then it's not there
        // and NetworkInterface.getByInetAddress(...) returns null 'cause it
        // cannot find an Interface for it. The following checks take care
        // of this case.
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return true;
        }
        
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException err) {
            return false;
        }
    }
    
    /**
     * Returns true if the SocketAddress is any of our local machine addresses
     */
    public static boolean isLocalAddress(SocketAddress addr) {
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        return !iaddr.isUnresolved() && isLocalAddress(iaddr.getAddress());
    }
    
    /**
     * Returns whether or not the two ip addresses share the same
     * first octet in their address.  
     * 
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in an IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isCloseIP(InetAddress addr0, InetAddress addr1) {
        return isCloseIP(addr0.getAddress(), addr1.getAddress());
    }
    
    /**
     * Returns whether or not the two ip addresses share the same
     * first octet in their address.  
     * 
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in an IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isCloseIP(byte[] addr0, byte[] addr1) {
        if ((isIPv4Address(addr0) && isIPv4Address(addr1)) 
                || (isIPv4MappedAddress(addr0) && isIPv4MappedAddress(addr1))) {
            return addr0[/* 0 */ addr0.length - 4] == addr1[/* 0 */ addr1.length - 4];                    
        }
        return false;
    }
    
    /**
     * Returns whether or not the two ip addresses share the same
     * first two octets in their address -- the most common
     * indication that they may be on the same network.
     *
     * Private networks are NOT CONSIDERED CLOSE.
     *
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in a true IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        // if 0 is not a private address but 1 is, then the next
        // check will fail anyway, so this is okay.
        if ( isPrivateAddress(addr0) ) {
            return false;
            
        } else if ((isIPv4Address(addr0) && isIPv4Address(addr1)) 
                || (isIPv4MappedAddress(addr0) && isIPv4MappedAddress(addr1))) {
            
            return addr0[/* 0 */ addr0.length - 4] == addr1[/* 0 */ addr1.length - 4]
                && addr0[/* 1 */ addr0.length - 3] == addr1[/* 1 */ addr1.length - 3];
        }
        return false;
    }
    
    /**
     * Returns whether or not the given ip address shares the same
     * first three octets as the address for this node -- the most 
     * common indication that they may be on the same network.
     *
     * @param addr the address to compare
     */
    public static boolean isVeryCloseIP(InetAddress addr) {
        return isVeryCloseIP(addr.getAddress());
    }
    
    /**
     * Returns whether or not the given ip address shares the same
     * first three octets as the address for this node -- the most 
     * common indication that they may be on the same network.
     *
     * @param addr the address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr) {
        return isVeryCloseIP(LocalSocketAddressService.getLocalAddress(), addr);
    }
    
    /**
     * Returns whether or not this node has a private address.
     *
     * @return <tt>true</tt> if this node has a private address,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivate() {
        return isPrivateAddress(LocalSocketAddressService.getLocalAddress());
    }
    
    /**
     * Utility method for determing whether or not the given 
     * address is private taking an InetAddress object as argument
     * like the isLocalAddress(InetAddress) method. 
     *
     * This method is IPv6 compliant
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivateAddress(InetAddress address) {
        if (!LocalSocketAddressService.isLocalAddressPrivate()) {
            return false;
        }
        
        if (address.isAnyLocalAddress() 
                || address.isLoopbackAddress() 
                || address.isLinkLocalAddress() 
                || address.isSiteLocalAddress()
                || isUniqueLocalUnicastAddress(address)
                || isBroadcastAddress(address)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if the given address is a private address.
     * 
     * This method is IPv6 compliant
     * 
     * @param address the address to check
     * @param localIsPrivate whether or not private addresses are
     *          considered private (in other words this method will
     *          always return false if localIsPrivate is false).
     */
    public static boolean isPrivateAddress(byte[] address) {
        if (!LocalSocketAddressService.isLocalAddressPrivate()) {
            return false;
        }
        
        if (isAnyLocalAddress(address) 
                || isLoopbackAddress(address) 
                || isLinkLocalAddress(address) 
                || isSiteLocalAddress(address)
                || isUniqueLocalUnicastAddress(address)
                || isBroadcastAddress(address)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Utility method for determing whether or not the given 
     * address is private.  Delegates to 
     * <tt>isPrivateAddress(InetAddress)</tt>.
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
     * Utility method for determing whether or not the given 
     * address is private taking an InetAddress object as argument
     * like the isLocalAddress(InetAddress) method. Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    public static boolean isPrivateAddress(SocketAddress address) {
        return isPrivateAddress(((InetSocketAddress)address).getAddress());
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
        StringBuilder sbuf = new StringBuilder(16);   
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
        try {
            return isMe(InetAddress.getByName(host).getAddress(), port);
        } catch (UnknownHostException e) {
            return false;
        }
    }
    
    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>false</tt>.
     */
    public static boolean isMe(byte[] address, int port) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "127.*.*.*" since
        //they are aliases this machine.

        if (isLoopbackAddress(address)) {
            return port == LocalSocketAddressService.getLocalPort();
        } else {
            byte[] local = LocalSocketAddressService.getLocalAddress();
            return port == LocalSocketAddressService.getLocalPort() 
                    && Arrays.equals(address, local);
        }
    }
    
    /**
     * Returns true if the given IpPort is the local host
     */
    public static boolean isMe(IpPort me) {
    	if (me == IpPortForSelf.instance())
    	    return true;
    	return isMe(me.getInetAddress().getAddress(), me.getPort());
    }

    /**
     * Determines if the given socket is from a local host.
     * 
     * This method is IPv6 compliant
     */
    public static boolean isLocalHost(Socket socket) {
        return isLocalAddress(socket.getInetAddress());
    }
    
    /**
     * Packs a Collection of IpPorts into a byte array.
     */
    public static byte[] packIpPorts(Collection<? extends IpPort> ipPorts) {
        byte[] data = new byte[ipPorts.size() * 6];
        int offset = 0;
        for(IpPort next : ipPorts) {
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
     * @throws InvalidDataException if an invalid Ip is found or the size 
     * is not divisble by six
     */
    public static List<IpPort> unpackIps(byte [] data) throws InvalidDataException {
    	if (data.length % 6 != 0)
    		throw new InvalidDataException("invalid size");
    	
    	int size = data.length/6;
    	List<IpPort> ret = new ArrayList<IpPort>(size);
    	byte [] current = new byte[6];
    	
    	
    	for (int i=0;i<size;i++) {
    		System.arraycopy(data,i*6,current,0,6);
    		ret.add(IPPortCombo.getCombo(current));
    	}
    	
    	return Collections.unmodifiableList(ret);
    }
    
    /**
     * Filters unique IPs based on a Class C Netmask
     */
    public static <T extends IpPort> Collection<T> filterOnePerClassC(Collection<T> c) {
        return filterUnique(c, CLASS_C_NETMASK);
    }
    
    /**
     * Filters unique ips based on a netmask.
     */
    public static <T extends IpPort> Collection<T> filterUnique(Collection<T> c, int netmask) {
        ArrayList<T> ret = new ArrayList<T>(c.size());
        Set<Integer> ips = new HashSet<Integer>();
        for (T ip : c) {
            if (ips.add( getMaskedIP(ip.getInetAddress(), netmask) ))
                ret.add(ip);
            
        }
        ret.trimToSize();
        return ret;
    }
    
    /**
     * Applies the netmask on the lower four bytes of the given 
     * InetAddress and returns it as an Integer.
     * 
     * This method is IPv6 compliant but shouldn't be called if
     * the InetAddress is neither IPv4 compatible nor mapped!
     */
    public static int getMaskedIP(InetAddress addr, int netmask) {
        byte[] address = addr.getAddress();
        return ByteOrder.beb2int(address, /* 0 */ address.length - 4) & netmask;
    }
    
    /**
     * @return A non-loopback IPv4 address of a network interface on the local
     *         host.
     * @throws UnknownHostException
     */
    public static InetAddress getLocalAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        
        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
            return addr;
        }
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
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
    
    /**
     * Retuens the IP:Port as byte array.
     * 
     * This method is IPv6 compliant
     */
    public static byte[] getBytes(SocketAddress addr) throws UnknownHostException {
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        if (iaddr.isUnresolved()) {
            throw new UnknownHostException(iaddr.toString());
        }
        
        return getBytes(iaddr.getAddress(), iaddr.getPort());
    }
    
    /**
     * Retuens the IP:Port as byte array.
     * 
     * This method is IPv6 compliant
     */
    public static byte[] getBytes(IpPort ipp) {
        return getBytes(ipp.getInetAddress(), ipp.getPort());
    }
    
    /**
     * Returns the IP:Port as byte array.
     * 
     * This method is IPv6 compliant
     */
    public static byte[] getBytes(InetAddress addr, int port) {
        if (!isValidPort(port)) {
            throw new IllegalArgumentException("Port out of range: " + port);
        }
        
        byte[] address = addr.getAddress();

        byte[] dst = new byte[address.length + 2];
        System.arraycopy(address, 0, dst, 0, address.length);
        dst[dst.length-2] = (byte)((port >> 8) & 0xFF);
        dst[dst.length-1] = (byte)((port     ) & 0xFF);
        return dst;
    }
    
    /**
     * Returns true if both SocketAddresses are either IPv4 or IPv6 addresses
     * 
     * This method is IPv6 compliant
     */
    public static boolean isSameAddressSpace(SocketAddress a, SocketAddress b) {
        return isSameAddressSpace(
                    ((InetSocketAddress)a).getAddress(), 
                    ((InetSocketAddress)b).getAddress());
    }
    
    /**
     * Returns true if both InetAddresses are compatible (IPv4 and IPv4
     * or IPv6 and IPv6).
     * 
     * This method is IPv6 compliant
     */
    public static boolean isSameAddressSpace(InetAddress a, InetAddress b) {
        if (a == null || b == null) {
            return false;
        }
        
        // Both are either IPv4 or IPv6
        if ((a instanceof Inet4Address && b instanceof Inet4Address)
                || (a instanceof Inet6Address && b instanceof Inet6Address)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns the IPv6 address bytes of IPv6 and IPv4 IP addresses. 
     * @throws IllegalArgumentException if given a different address type
     */
    public static byte[] getIPv6AddressBytes(InetAddress address) {
        byte[] bytes = address.getAddress();
        switch (bytes.length) {
            case 16:
                // Return the IPv6 address
                return bytes;
            case 4:
                // Turn the IPv4 address into a IPv4 mapped IPv6
                // address
                byte[] result = new byte[16];
                result[10] = (byte) 0xff;
                result[11] = (byte) 0xff;
                System.arraycopy(bytes, 0, result, 12, bytes.length);
                return result;
            default:
                throw new IllegalArgumentException("unhandled address length");
        }
    }
    
    /**
     * Returns true if an IPv6 representation of <code>address</code> exists.
     */
    public static boolean isIPv6Compatible(InetAddress address) {
        int length = address.getAddress().length;
        return length == 4 || length == 16;
    }
    
    /**
     * Returns true if the given byte-array is an IPv4 address
     */
    private static boolean isIPv4Address(byte[] address) {
        return address.length == 4;
    }
    
    /**
     * Returns true if the given byte-array is an IPv4 compatible address.
     * They're used when IPv6 systems need to communicate with each other, 
     * but are separated by an IPv4 network.
     */
    static boolean isIPv4CompatibleAddress(byte[] address) { 
        // Is it a IPv4 compatible IPv6 address?
        // (copied from Inet6Address)
        if (address.length == 16 
                && (address[ 0] == 0x00) && (address[ 1] == 0x00) 
                && (address[ 2] == 0x00) && (address[ 3] == 0x00) 
                && (address[ 4] == 0x00) && (address[ 5] == 0x00) 
                && (address[ 6] == 0x00) && (address[ 7] == 0x00) 
                && (address[ 8] == 0x00) && (address[ 9] == 0x00) 
                && (address[10] == 0x00) && (address[11] == 0x00))  {   
            return true;
        }
        
        return false;  
    }
    
    /**
     * Returns true if the given byte-array is an IPv4 mapped address.
     * IPv4 mapped addresses indicate systems that do not support IPv6. 
     * They are limited to IPv4. An IPv6 host can communicate with an 
     * IPv4 only host using the IPv4 mapped IPv6 address.
     */
    static boolean isIPv4MappedAddress(byte[] address) {
        if (address.length == 16 
                && (address[ 0] == 0x00) && (address[ 1] == 0x00) 
                && (address[ 2] == 0x00) && (address[ 3] == 0x00) 
                && (address[ 4] == 0x00) && (address[ 5] == 0x00) 
                && (address[ 6] == 0x00) && (address[ 7] == 0x00) 
                && (address[ 8] == 0x00) && (address[ 9] == 0x00) 
                && (address[10] == (byte)0xFF) && (address[11] == (byte)0xFF)) {   
            return true;
        }
        
        return false;  
    }
    
    /**
     * Returns true if the given byte-array is an any local address.
     */
    static boolean isAnyLocalAddress(byte[] address) {
        if (address.length == 4 || address.length == 16) {
            byte test = 0;
            for (int i = 0; i < address.length; i++) {
                test |= address[i];
            }
            
            return (test == 0x00);
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a loopback address
     */
    static boolean isLoopbackAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return (address[/* 0 */ address.length - 4] & 0xFF) == 127;
        } else if (address.length == 16) {
            byte test = 0x00;
            for (int i = 0; i < 15; i++) {
                test |= address[i];
            }
            return (test == 0x00) && (address[15] == 0x01);
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a link-local address
     */
    static boolean isLinkLocalAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return (address[/* 0 */ address.length - 4] & 0xFF) == 169
                && (address[/* 1 */ address.length - 3] & 0xFF) == 254;
            
        // FE80::/64
        } else if (address.length == 16) {
            return (address[0] & 0xFF) == 0xFE
                && (address[1] & 0xC0) == 0x80;
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a site-local address.
     * IPv6 site-local addresses were deprecated in September 2004 
     * by RFC 3879 and replaced by RFC 4193 (Unique Local IPv6 Unicast
     * Addresses).
     */
    static boolean isSiteLocalAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return  (address[/* 0 */ address.length - 4] & 0xFF) == 10
                || ((address[/* 0 */ address.length - 4] & 0xFF) == 172
                &&  (address[/* 1 */ address.length - 3] & 0xF0) == 16)
                || ((address[/* 0 */ address.length - 4] & 0xFF) == 192
                &&  (address[/* 1 */ address.length - 3] & 0xFF) == 168);
            
        // Has been deprecated in September 2004 by RFC 3879 
        // FEC0::/10
        } else if (address.length == 16) {
            return (address[0] & 0xFF) == 0xFE
                && (address[1] & 0xC0) == 0xC0;
        }
        return false;
    }
    
    /**
     * Returns true if the given InetAddress is an Unique Local IPv6
     * Unicast Address. See RFC 4193 for more info.
     */
    public static boolean isUniqueLocalUnicastAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            return isUniqueLocalUnicastAddress(address.getAddress());
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is an Unique Local IPv6
     * Unicast Address. See RFC 4193 for more info.
     */
    private static boolean isUniqueLocalUnicastAddress(byte[] address) {
        // FC00::/7
        if (address.length == 16) {
            return (address[0] & 0xFE) == 0xFC;
        }
        return false;
    }

    /**
     * Returns true if the given InetAddress is a broadcast address.
     */
    public static boolean isBroadcastAddress(InetAddress address) {
        return isBroadcastAddress(address.getAddress());
    }
    
    /**
     * Returns true if the given byte-array is a brodcast address
     * 
     * This method is IPv6 compliant but returns always false if
     * the given address is neither a true IPv4, nor an IPv4-mapped
     * address.
     */
    private static boolean isBroadcastAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return (address[/* 0 */ address.length - 4] & 0xFF) == 0xFF;
        }
        
        return false;
    }
    
    /**
     * Returns true if the given InetAddress is a private IPv4-compatible
     * address.
     * 
     * It checks for a somewhat tricky and undefined case. An address such
     * as ::0000:192.168.0.1 is an IPv6 address, it's an IPv4-compatible
     * address but it's by IPv6 definition not a site-local (private) address.
     * On the other hand it's a private IPv4 address.
     */
    public static boolean isPrivateIPv4CompatibleAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            return isPrivateIPv4CompatibleAddress(address.getAddress());
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a private IPv4-compatible
     * address.
     */
    private static boolean isPrivateIPv4CompatibleAddress(byte[] address) {
        if (isIPv4CompatibleAddress(address)) {
            // Copy the lower four bytes and perform the
            // checks on it to determinate whether or not
            // it's a private IPv4 address
            byte[] ipv4 = new byte[4];
            System.arraycopy(address, 12, ipv4, 0, ipv4.length);
            return isPrivateAddress(ipv4);
        }
        return false;
    }
    
    /**
     * Returns true if the given InetAddress has a prefix that's used in
     * Documentation. See RFC 3849 for more information.
     */
    public static boolean isDocumentationAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            return isDocumentationAddress(address.getAddress());
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array has a prefix that's used in
     * Documentation. See RFC 3849 for more information.
     */
    private static boolean isDocumentationAddress(byte[] address) {
        // 2001:0DB8::/32
        if (address.length == 16) {
            return (address[0] & 0xFF) == 0x20
                && (address[1] & 0xFF) == 0x01
                && (address[2] & 0xFF) == 0x0D
                && (address[3] & 0xFF) == 0xB8;
        }
        return false;
    }
}
