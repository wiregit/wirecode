package com.limegroup.gnutella.routing;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.messages.*;
import java.io.*;

/**
 * Unit tests for RouteTableMessage
 */
public class RouteTableMessageTest extends BaseTestCase {
        
	public RouteTableMessageTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(RouteTableMessageTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
        //Read bytes with bad variant
        byte[] message=new byte[23+2];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)3;                                 //TTL
        message[19]=(byte)2;                                 //payload length
        message[23+0]=(byte)0xFF;                            //bogus variant
        InputStream in=new ByteArrayInputStream(message);
        try {
            Message m=(ResetTableMessage)Message.read(in);
            fail("exception should have been thrown");
        } catch (BadPacketException e) {
        }
    }
}    