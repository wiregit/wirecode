package com.limegroup.gnutella.util;

import com.limegroup.gnutella.settings.*;
import junit.framework.*;
import com.sun.java.util.collections.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;

/**
 * Tests certain features of NetworkUtils
 */
public class NetworkUtilsTest extends com.limegroup.gnutella.util.BaseTestCase {

    public NetworkUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NetworkUtilsTest.class);
    }  

    public void testIsPrivateAddress() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
        byte[] address = new byte[4];
        
        address = InetAddress.getByName("2.32.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("1.32.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("127.0.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("127.2.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("192.168.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("172.16.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("172.31.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("169.254.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("0.254.0.1").getAddress();
        assertTrue("should be a private address"+address,
                   NetworkUtils.isPrivateAddress(address));

        //address = InetAddress.getByName("240.254.0.1").getAddress();
        //assertTrue("should be a private address"+address,
        //         NetworkUtils.isPrivateAddress(address));

        //address = InetAddress.getByName("255.254.0.1").getAddress();
        //assertTrue("should be a private address"+address,
        //         NetworkUtils.isPrivateAddress(address));


        // try boundary cases to make sure they pass
        address = InetAddress.getByName("172.15.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("172.32.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("192.167.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("192.169.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("239.32.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("1.32.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));

        address = InetAddress.getByName("180.32.0.1").getAddress();
        assertTrue("should not be a private address"+address,
                   !NetworkUtils.isPrivateAddress(address));
    }

	public void testNetworkUtilsPortCheck() {
		int port = -1;
		assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
		port = 99999999;
		assertTrue("port should not be valid", !NetworkUtils.isValidPort(port));
		port = 20;
		assertTrue("port should be valid", NetworkUtils.isValidPort(port));
	}
}
