package org.limewire.io;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


/**
 * Tests certain features of NetworkUtils
 */
public class NetworkUtilsTest extends BaseTestCase {

    /**
     * Array of private addresses for testing.
     */
    private static final String[] PRIVATE_ADDRESSES = {
        "0.0.0.0",
        "127.1.1.1",
        "127.0.0.0",
        "172.16.0.0",
        "172.31.255.255",
        "172.17.0.255",
        "192.168.0.0",
        "192.168.0.1",
        "192.168.255.255",
        "169.254.0.0",
        "169.254.255.255",
        "255.1.2.3",
        "255.0.0.0"
        //"240.0.0.0",
    };

    /**
     * Array of public addresses for testing.
     */
    private static final String[] PUBLIC_ADDRESSES = {
        "2.32.0.1",
        "20.43.2.1",
        "1.32.0.1",
        "172.15.0.1",
        "172.32.0.1",
        "192.167.0.1",
        "192.169.0.1",
        "1.0.0.0",
        "180.32.0.1",
        "239.32.0.1",
        "128.0.0.1",
        "169.253.0.0",
        "169.255.0.0",
    };
    
    private static LocalSocketAddressProvider defaultProvider;
    
    private static LocalSocketAddressProviderStub stubProvider;

    public NetworkUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NetworkUtilsTest.class);
    }
    
    public void setUp() {
        defaultProvider = LocalSocketAddressService.getSharedProvider();
        stubProvider = new LocalSocketAddressProviderStub();
        LocalSocketAddressService.setSocketAddressProvider(stubProvider);
    }
    
    public void tearDown() {
        LocalSocketAddressService.setSocketAddressProvider(defaultProvider);
    }

    /**
     * Tests the method for checking whether or not an IP address is
     * "close" to this address.
     */
    public void testIsCloseIP() throws Exception {
        byte[] addr0 = new byte[4];
        byte[] addr1 = new byte[4];

        addr0[0] = (byte)3;
        addr1[0] = (byte)3;
        addr0[1] = (byte)2;
        addr1[1] = (byte)2;
        assertTrue("should be considered close", 
                   NetworkUtils.isCloseIP(addr0, addr1));

        addr0[1] = (byte)4;
        assertTrue("should be considered close", 
                   NetworkUtils.isCloseIP(addr0, addr1));

        addr0[0] = (byte)0;
        assertTrue("should not be considered close", 
                   !NetworkUtils.isCloseIP(addr0, addr1));
    }
    
    public void testFilterUnique() throws Exception {
        List<IpPortImpl> l = new ArrayList<IpPortImpl>();
        l.add(new IpPortImpl("1.1.1.1",2));
        l.add(new IpPortImpl("1.1.1.2",2));
        l.add(new IpPortImpl("1.1.1.3",2));
        l.add(new IpPortImpl("1.1.2.3",2));
        
        Collection<IpPortImpl> ret = NetworkUtils.filterUnique(l, 0xFF000000);
        assertEquals(1, ret.size());
        assertEquals(1, NetworkUtils.filterUnique(l, 0xFFFF0000).size());
        assertEquals(2, NetworkUtils.filterUnique(l, 0xFFFFFF00).size());
        assertEquals(4, NetworkUtils.filterUnique(l, 0xFFFFFFFF).size());
    }

    /**
     * Tests the method for checking if something's from a private
     * network with a string argument.
     */
    public void testIsPrivateAddressWithString() throws Exception {
        stubProvider.setLocalAddressPrivate(true);
        for(int i=0; i<PUBLIC_ADDRESSES.length; i++) {
            String address = PUBLIC_ADDRESSES[i];
            assertTrue("should not be a private address: "+address, 
                       !NetworkUtils.isPrivateAddress(address));
        }

        for(int i=0; i<PRIVATE_ADDRESSES.length; i++) {
            String address = PRIVATE_ADDRESSES[i];
            assertTrue("should be a private address: "+address, 
                       NetworkUtils.isPrivateAddress(address));
        }
    }

    /**
     * Tests the method for whether or not an address is private,
     * with the address is bytes.
     */
    public void testIsPrivateAddress() throws Exception {
        stubProvider.setLocalAddressPrivate(true);
        byte[] address = new byte[4];
        
        for(int i=0; i<PUBLIC_ADDRESSES.length; i++) {
            address = 
                InetAddress.getByName(PUBLIC_ADDRESSES[i]).getAddress();
            assertFalse("should be a public address"+address,
                       NetworkUtils.isPrivateAddress(address));
        }

        for(int i=0; i<PRIVATE_ADDRESSES.length; i++) {
            address = 
                InetAddress.getByName(PRIVATE_ADDRESSES[i]).getAddress();
            assertTrue("should be a private address"+address,
                       NetworkUtils.isPrivateAddress(address));
        }
    }
    
    public void testIsInvalidAddressWithIP() throws Exception {
        IP ip = new IP("0.0.0.0");
        assertFalse(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("1.2.3.0");
        assertTrue(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("255.0.1.1");
        assertFalse(NetworkUtils.isValidAddress(ip));
        
        ip = new IP("1.2.3.255");
        assertTrue(NetworkUtils.isValidAddress(ip));
    }

    /**
     * Test to make sure the method for checking for valid ports is working.
     */
	public void testNetworkUtilsPortCheck() {
		int port = -1;
		assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
        port = 0;
        assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
		port = 99999999;
		assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
		port = 20;
		assertTrue("port should be valid", NetworkUtils.isValidPort(port));
	}

    /**
     * Tests the ip2string method.
     */
    public void testIP2String() throws Exception {
        byte[] buf=new byte[10];
        buf[3]=(byte)192;
        buf[4]=(byte)168;
        buf[5]=(byte)0;
        buf[6]=(byte)1;       
        assertEquals("192.168.0.1", NetworkUtils.ip2string(buf, 3));
        
        buf=new byte[4];
        buf[0]=(byte)0;
        buf[1]=(byte)1;
        buf[2]=(byte)2;
        buf[3]=(byte)3;
        assertEquals("0.1.2.3", NetworkUtils.ip2string(buf));

        buf=new byte[4];
        buf[0]=(byte)252;
        buf[1]=(byte)253;
        buf[2]=(byte)254;
        buf[3]=(byte)255;
        assertEquals("252.253.254.255",NetworkUtils.ip2string(buf));        
    }
    
    /**
     * Tests the isMe method.
     */
    public void testIsMe() throws Exception {
        stubProvider.setLocalPort(6346);
        assertTrue(NetworkUtils.isMe("localhost", 6346));
        assertTrue(NetworkUtils.isMe("127.1.2.1", 6346));            
        assertTrue(NetworkUtils.isMe(new byte[] { (byte)127, 0, 0, 0 }, 6346));
        
        stubProvider.setLocalPort(6345);
        assertFalse(NetworkUtils.isMe("localhost", 6346));
        assertFalse(NetworkUtils.isMe("127.1.2.1", 6346));            
        assertFalse(NetworkUtils.isMe(new byte[] { (byte)127, 0, 0, 0 }, 6346));
        
        stubProvider.setLocalPort(6346);
        stubProvider.setLocalAddress(new byte[] {(byte)123, (byte)132, (byte)231, 0});
        assertTrue(NetworkUtils.isMe("123.132.231.0", 6346));        
        assertTrue(NetworkUtils.isMe(new byte[] {(byte)123, (byte)132, (byte)231, 0}, 6346));

        assertFalse(NetworkUtils.isMe("123.132.231.1", 6346));        
        assertFalse(NetworkUtils.isMe(new byte[] {(byte)123, (byte)132, (byte)231, 1}, 6346));
        
    }
    
    public void testGetAddressV6Bytes() throws UnknownHostException {
        
        for (String name : new String[] { "127.0.0.1", "255.255.255.0", 
                "192.168.0.1", "128.0.245.90" }) {
            InetAddress addr = InetAddress.getByName(name);
            byte[] bytes = NetworkUtils.getIPv6AddressBytes(addr);
            assertEquals(16, bytes.length);
            InetAddress res = InetAddress.getByAddress(bytes); 
            assertTrue(res instanceof Inet4Address);
            assertEquals(addr, res);
        }
        
        for (String name : new String[] { "[::]", "[::1]", "[2001:db8::]", 
                "[2001:0db8:0000:0000:0000:0000:1428:57ab]",
                "[2001:0db8::1428:57ab]"}) {
            InetAddress addr = InetAddress.getByName(name);
            byte[] bytes = NetworkUtils.getIPv6AddressBytes(addr);
            assertEquals(addr.getAddress(), bytes);
            InetAddress res = InetAddress.getByAddress(bytes);
            assertTrue(res instanceof Inet6Address);
            assertEquals(addr, res);
        }
    }
    
    public void testIsPrivateAddressIPv6() throws UnknownHostException {
        stubProvider.setLocalAddressPrivate(true);
        
        // Private IPv4 mapped address
        InetAddress addr1 = InetAddress.getByName("[::ffff:192.168.1.0]");
        assertInstanceof(Inet4Address.class, addr1);
        assertTrue(NetworkUtils.isPrivateAddress(addr1));
        
        // Private IPv4 compatible address
        InetAddress addr2 = InetAddress.getByName("[::0000:192.168.1.0]");
        assertInstanceof(Inet6Address.class, addr2);
        assertTrue(NetworkUtils.isPrivateAddress(addr2));
        
        // Public IPv4 mapped address
        InetAddress addr3 = InetAddress.getByName("[::ffff:216.254.98.132]");
        assertInstanceof(Inet4Address.class, addr3);
        assertFalse(NetworkUtils.isPrivateAddress(addr3));
        
        // Public IPv4 compatible address
        InetAddress addr4 = InetAddress.getByName("[::0000:216.254.98.132]");
        assertInstanceof(Inet6Address.class, addr4);
        assertFalse(NetworkUtils.isPrivateAddress(addr4));
    }
    
    public void testIsIPv4ComatibleAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.168.1.0");
        
        // Should be an IPv4 compatible addresses
        assertTrue(NetworkUtils.isIPv4CompatibleAddress(addr.getAddress()));
        
        // Create an IPv4 compatible IPv6 address
        byte[] compatible = new byte[16];
        System.arraycopy(addr.getAddress(), 0, compatible, 12, addr.getAddress().length);
        assertTrue(NetworkUtils.isIPv4CompatibleAddress(compatible));
        
        // Change any bytes from 0 through 11 and it shouldn't
        // be any longer an IPv4 compatible address
        compatible[10] = (byte)0xFF;
        compatible[11] = (byte)0xFF;
        assertFalse(NetworkUtils.isIPv4CompatibleAddress(compatible));
    }
    
    public void testIsIPv4MappedAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.168.1.0");
        assertTrue(NetworkUtils.isIPv4MappedAddress(addr.getAddress()));
        
        // Create an IPv4 mapped IPv6 address. We start with
        // a byte-array that's NOT an IPv4 mapped address!
        byte[] mapped = new byte[16];
        System.arraycopy(addr.getAddress(), 0, mapped, 12, addr.getAddress().length);
        assertFalse(NetworkUtils.isIPv4MappedAddress(mapped));
        
        // Fix the address and it should be an IPv4 mapped
        // IPv6 address now!
        mapped[10] = (byte)0xFF;
        mapped[11] = (byte)0xFF;
        assertTrue(NetworkUtils.isIPv4MappedAddress(mapped));
    }
    
    public void testIsSameAddressSpace() throws UnknownHostException {
        InetAddress addr1 = InetAddress.getByName("192.168.1.1");
        InetAddress addr2 = InetAddress.getByName("192.168.1.2");
        
        // Both instances should be IP4
        assertInstanceof(Inet4Address.class, addr1);
        assertInstanceof(Inet4Address.class, addr2);
        
        // IPv4 with IPv4
        assertTrue(NetworkUtils.isSameAddressSpace(addr1, addr2));
        
        InetAddress addr3 = InetAddress.getByName("[::AAAA:192.168.1.3]");
        InetAddress addr4 = InetAddress.getByName("[::BBBB:192.168.1.4]");
        
        // Both instances should be IPv6
        assertInstanceof(Inet6Address.class, addr3);
        assertInstanceof(Inet6Address.class, addr4);
        
        // IPv6 with IPv6
        assertTrue(NetworkUtils.isSameAddressSpace(addr3, addr4));
        
        // IPv4 with IPv6
        assertFalse(NetworkUtils.isSameAddressSpace(addr1, addr3));
    }
    
    public void testIsLocalAddress() throws UnknownHostException, SocketException {
        InetAddress addr1 = InetAddress.getByName("localhost");
        assertTrue(NetworkUtils.isLocalAddress(addr1));
        
        InetAddress addr2 = InetAddress.getByName("127.0.0.1");
        assertTrue(NetworkUtils.isLocalAddress(addr2));
        
        InetAddress addr3 = InetAddress.getLocalHost();
        assertTrue(NetworkUtils.isLocalAddress(addr3));
        
        // Get all local InetAddresses for localhost
        InetAddress[] addr4 = InetAddress.getAllByName("localhost");
        assertTrue(addr4.length > 0);
        for (InetAddress addr : addr4) {
            assertTrue(NetworkUtils.isLocalAddress(addr));
        }
        
        // Go through every NetworkInterface and every InetAddress
        // They should be all local addresses!
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        boolean checkedAtLeastOneAddress = false;
        while(nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();
            Enumeration<InetAddress> addresses = nif.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                assertTrue(NetworkUtils.isLocalAddress(address));
                checkedAtLeastOneAddress = true;
            }
        }
        assertTrue(checkedAtLeastOneAddress);
        
        // This is not a local address of this machine
        InetAddress addr5 = InetAddress.getByName("www.google.com");
        assertFalse(NetworkUtils.isLocalAddress(addr5));
        
        // Nor is this
        InetAddress addr6 = InetAddress.getByName("192.168.88.44");
        assertFalse(NetworkUtils.isLocalAddress(addr6));
    }
}
