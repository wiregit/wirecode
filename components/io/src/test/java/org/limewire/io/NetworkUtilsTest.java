package org.limewire.io;

import java.net.InetAddress;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.io.NetworkUtils;
import org.limewire.util.BaseTestCase;


import junit.framework.Test;


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
            assertTrue("should not be a private address"+address,
                       !NetworkUtils.isPrivateAddress(address));
        }

        for(int i=0; i<PRIVATE_ADDRESSES.length; i++) {
            address = 
                InetAddress.getByName(PRIVATE_ADDRESSES[i]).getAddress();
            assertTrue("should be a private address"+address,
                       NetworkUtils.isPrivateAddress(address));
        }
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
}
